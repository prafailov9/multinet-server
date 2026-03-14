package com.ntros.model.world.engine.d2.grid.fallingsand;

import static com.ntros.model.world.state.grid.CellType.ACID;
import static com.ntros.model.world.state.grid.CellType.ASH;
import static com.ntros.model.world.state.grid.CellType.EMPTY;
import static com.ntros.model.world.state.grid.CellType.FIRE;
import static com.ntros.model.world.state.grid.CellType.OBSIDIAN;
import static com.ntros.model.world.state.grid.CellType.OIL;
import static com.ntros.model.world.state.grid.CellType.SAND;
import static com.ntros.model.world.state.grid.CellType.STONE;
import static com.ntros.model.world.state.grid.CellType.SMOKE;
import static com.ntros.model.world.state.grid.CellType.WATER;

import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.engine.core.SimulationGridEngine;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.state.grid.CellType;
import com.ntros.model.world.state.grid.GridSnapshot;
import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.core.SimulationGridState;
import com.ntros.model.world.state.grid.FallingSandState;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;

/**
 * Falling Sand cellular-automaton engine.
 *
 * <h3>Buffer ownership</h3>
 * The engine owns the authoritative world state as two flat {@code CellType[]} arrays
 * ({@code current} / {@code next}) indexed by {@code y * W + x}.  The {@link GridState}
 * terrain map is never read or written during simulation; it is intentionally left
 * unsupported in {@link FallingSandState}.
 * The sync point is {@link #snapshot(GridState)}, which builds a transient terrain view
 * from {@code current[]} only when a broadcast frame is due (at broadcast rate, not sim rate).
 *
 * <h3>Double-buffer strategy</h3>
 * Each tick, rules write results into {@code next[]}.  After all cells are processed,
 * {@code current} and {@code next} are swapped — zero allocation, zero extra fill.
 *
 * <h3>Parallel metadata</h3>
 * {@code metadata[]} is a per-cell {@code int} array that carries material-specific state
 * not representable by the {@link CellType} alone:
 * <ul>
 *   <li><b>FIRE:</b> remaining lifetime in ticks (counts down to 0 → becomes ASH)</li>
 *   <li><b>ACID:</b> erosion strength (could vary per particle)</li>
 * </ul>
 *
 * <h3>Player model</h3>
 * Connecting clients are registered as <em>observers</em>. They appear in
 * {@code takenPositions} and do not interact with the cell grid.
 */
@Slf4j
public class FallingSandEngine implements SimulationGridEngine {

  // ── Engine buffers (owned by this engine, not by GridState) ───────────────

  /**
   * Live cell grid — authoritative world state.
   */
  private CellType[] current;

  /**
   * Scratch buffer written by rule methods each tick, then swapped with {@code current}.
   * Always contains {@link CellType#EMPTY} at the start of each tick (guaranteed by
   * {@link #prepareNextBuffer()}).
   */
  private CellType[] next;

  /**
   * Per-cell auxiliary data (fire TTL, acid strength, …).
   * Kept in sync with {@code current}: when a cell moves, its metadata moves with it.
   */
  private int[] metadata;
  private int[] nextMetadata;

  private static final int FIRE_TTL = 120;
  private static final float IGNITE_CHANCE = 0.06f;
  private static final float DISSOLVE_CHANCE = 0.7f;
  private static final int SMOKE_TTL_BASE = 10;
  private static final int SMOKE_TTL_VAR = 18;   // final TTL = base + rng(var) = 10..27
  private static final int ACID_FUME_TTL = 10;
  private static final float SMOKE_SPAWN_RATE = 0.5f; // max 50% chance per tick at full fire TTL
  private final Random rng = new Random();

  // helpers index arrays
  // all 8 neighbors
  private final int[] neighX = {-1, 0, 1, -1, 1, -1, 0, 1};
  private final int[] neighY = {-1, -1, -1, 0, 0, 1, 1, 1};

  // SMOKE has density -1 (same as EMPTY) so all gravity materials fall through it
  private final Map<CellType, Integer> densityMap = Map.of(STONE, 5, OBSIDIAN, 5, SAND, 4, ASH, 3,
      WATER, 2, ACID, 2, OIL, 1, FIRE, 0, EMPTY, -1, SMOKE, -1);

  private int W, H;

  /**
   * Monotonic tick counter. Used to alternate horizontal scan direction each tick so that
   * horizontal flow (water, oil, acid) does not develop a permanent left or right bias.
   */
  private long tick = 0L;

  private boolean needsFullSnapshot = true;

  @Override
  public void applyIntents(GridState state) {
    ensureBuffers(state);
    nextGeneration();
  }

  @Override
  public Object snapshot(GridState state) {
    needsFullSnapshot = false;
    return new GridSnapshot(buildTerrainView(), Map.of());
  }

  @Override
  public String serialize(GridState state) {
    return buildJson(false);
  }

  @Override
  public String serializeOneLine(GridState state) {
    return buildJson(true);
  }

  @Override
  public WorldResult orchestrate(OrchestrateRequest req, SimulationGridState state) {
    ensureBuffers(state);
    return switch (req.action()) {
      case PLACE -> applyPlace(req, state);
      case CLEAR -> applyClear(state);
      case RANDOM_SEED -> applyRandomSeed(state);
      default -> WorldResult.failed("orchestrator", state.worldName(),
          "Falling Sand supports PLACE, CLEAR, and RANDOM_SEED orchestration.");
    };
  }

  /**
   * Fills the grid with a randomised mix of materials.
   * Distribution: SAND 40%, STONE 20%, WATER 20%, OIL 10%, EMPTY 10%.
   */
  private WorldResult applyRandomSeed(GridState state) {
    CellType[] palette = {
        SAND, SAND, SAND, SAND,
        STONE, STONE,
        WATER, WATER,
        OIL,
        EMPTY
    };
    for (int i = 0; i < current.length; i++) {
      current[i] = palette[rng.nextInt(palette.length)];
      metadata[i] = 0;
    }
    needsFullSnapshot = true;
    log.info("[FallingSand] Random seed applied to '{}' ({}×{}).", state.worldName(), W, H);
    return WorldResult.succeeded("orchestrator", state.worldName(), "Random seed applied.");
  }

  private WorldResult applyPlace(OrchestrateRequest req, GridState state) {
    if (req.cells().isEmpty() || req.material() == null) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "PLACE requires a material and position.");
    }
    CellType mat;
    try {
      mat = CellType.valueOf(req.material());
    } catch (IllegalArgumentException e) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "Unknown material: " + req.material());
    }
    var pos = req.cells().getFirst();
    int x = (int) pos.getX();
    int y = (int) pos.getY();
    if (x < 0 || x >= W || y < 0 || y >= H) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "Position out of bounds: " + x + "," + y);
    }
    int i = y * W + x;
    current[i] = mat;
    metadata[i] = (mat == FIRE) ? FIRE_TTL : 0;
    needsFullSnapshot = true;
    return WorldResult.succeeded("orchestrator", state.worldName(),
        "Placed " + mat + " at " + x + "," + y);
  }

  private WorldResult applyClear(GridState state) {
    if (current != null) {
      Arrays.fill(current, CellType.EMPTY);
      Arrays.fill(metadata, 0);
    }
    needsFullSnapshot = true;
    log.info("[FallingSand] CLEAR — all cells erased.");
    return WorldResult.succeeded("orchestrator", state.worldName(), "World cleared.");
  }

  @Override
  public void reset(GridState state) {
    if (current != null) {
      Arrays.fill(current, CellType.EMPTY);
      Arrays.fill(next, CellType.EMPTY);
      Arrays.fill(metadata, 0);
      Arrays.fill(nextMetadata, 0);
    }
    needsFullSnapshot = true;
    log.info("[FallingSand] World reset — all cells cleared.");
  }

  private void ensureBuffers(GridState state) {
    int w = state.dimension().getWidth();
    int h = state.dimension().getHeight();
    if (current != null && W == w && H == h) {
      return;
    }
    W = w;
    H = h;
    int size = W * H;
    current = new CellType[size];
    next = new CellType[size];
    metadata = new int[size];
    nextMetadata = new int[size];
    Arrays.fill(current, CellType.EMPTY);
    Arrays.fill(next, CellType.EMPTY);
    needsFullSnapshot = true;
    log.debug("[FallingSand] Allocated {}×{} cell buffers ({} KB).", W, H, size * 5 / 1024);
  }

  private void prepareNextBuffer() {
    Arrays.fill(next, CellType.EMPTY);
    Arrays.fill(nextMetadata, 0);
  }

  private Map<Vector4, CellType> buildTerrainView() {
    Map<Vector4, CellType> view = new HashMap<>();
    for (int i = 0; i < current.length; i++) {
      CellType cell = current[i];
      if (cell != CellType.EMPTY) {
        view.put(Vector4.of(i % W, i / W, 0, 0), cell);
      }
    }
    return view;
  }

  private String buildJson(boolean oneLine) {
    String sep = oneLine ? "" : "\n";
    String indent = oneLine ? "" : "  ";
    StringBuilder sb = new StringBuilder("{").append(sep);
    sb.append(indent).append("\"tiles\": {").append(sep);
    boolean first = true;
    for (int i = 0; i < current.length; i++) {
      CellType cell = current[i];
      if (cell == CellType.EMPTY) {
        continue;
      }
      if (!first) {
        sb.append(",").append(sep);
      }
      first = false;
      sb.append(indent).append(indent).append('"').append(i % W).append(',').append(i / W)
          .append("\": ").append('"').append(cell.name()).append('"');
    }
    sb.append(sep).append(indent).append("}").append(sep).append("}");
    return sb.toString();
  }

  //  Scan order: iterate y from (H-1) down to 0 (bottom-to-top) for gravity materials
  //  (SAND, WATER, OIL, ACID, ASH).  Bottom-to-top guarantees that a grain that just
  //  fell to row y+1 will NOT be re-processed in the same tick — you already passed it.
  //
  //  For materials that spread sideways (WATER, OIL, ACID), alternate
  //  the x-scan direction on odd/even ticks using the `tick` field:
  //    int dir = (tick % 2 == 0) ? 1 : -1;
  //    int x   = (dir == 1) ? xi : W - 1 - xi;
  //
  //  FIRE: does NOT need a bottom-to-top scan (it rises). Iterate y top-to-bottom instead,
  //  or process fire in a separate pass.
  //
  //  A cell can displace another only if its own density is strictly greater.

  /**
   * Advances the simulation by one generation.
   *
   * <p>Suggested structure:
   * <ol>
   *   <li>Call {@link #prepareNextBuffer()} to clear {@code next[]}</li>
   *   <li>Scan y from H-1 down to 0; within each row scan x with alternating direction</li>
   *   <li>Dispatch each non-empty cell to its rule method</li>
   *   <li>Swap {@code current} ↔ {@code next} and increment {@code tick}</li>
   * </ol>
   */
  private void nextGeneration() {
    prepareNextBuffer();

    // Alternates x-scan direction each tick — prevents water/oil/acid
    // from always flowing left or always flowing right.
    int dir = (tick % 2 == 0) ? 1 : -1;

    // Bottom-to-top: a grain that falls from y to y+1 is already past;
    // it won't be picked up again in the same tick.
    for (int y = H - 1; y >= 0; y--) {
      for (int xi = 0; xi < W; xi++) {

        int x = (dir == 1) ? xi : W - 1 - xi;
        int idx = y * W + x;
        CellType cell = current[idx];
        switch (cell) {
          case SAND, ASH -> applyGravity(cell, idx, x, y);
          case WATER -> waterRules(cell, idx, x, y);
          case OIL -> oilRules(cell, idx, x, y);
          case FIRE -> fireSpread(x, y);
          case ACID -> acidRules(cell, idx, x, y);
          case STONE -> next[idx] = STONE; // stone destroyed by ACID
          case OBSIDIAN -> next[idx] = OBSIDIAN;
          case SMOKE -> smokeRise(x, y);
          default -> {
          } // EMPTY — nothing to do
        }
      }
    }

    // Swap buffers — no allocation
    CellType[] tmp = current;
    current = next;
    next = tmp;

    int[] m = metadata;
    metadata = nextMetadata;
    nextMetadata = m;

    tick++;
  }

  private void waterRules(CellType cell, int i, int x, int y) {
    liquidFall(cell, i, x, y);
    if (next[i] == cell) {
      moveToSides(cell, i, x, y);
    }
  }

  private void oilRules(CellType cell, int i, int x, int y) {
    waterRules(cell, i, x, y);
    if (next[i] == cell) {
      oilIgnition(i, x, y);
    }
  }

  private void acidRules(CellType cell, int i, int x, int y) {
    waterRules(cell, i, x, y);
    if (next[i] == cell) {
      acidDissolve(x,
          y); // always check neighbors — acid corrodes what it touches even while flowing
    }
  }


  private boolean isCombustible(CellType neighbour) {
    return neighbour == OIL;
  }

  private boolean isDissolvable(CellType neighbour) {
    return neighbour == STONE || neighbour == ASH;
  }

  private int idx(int x, int y) {
    return y * W + x;
  }

  private boolean inBounds(int x, int y) {
    return x >= 0 && x < W && y >= 0 && y < H;
  }


  private void write(int i, int j) {
    next[j] = current[i]; // cell moves to j
    next[i] = current[j]; // displaced cell stays at i
    nextMetadata[j] = metadata[i];
    nextMetadata[i] = metadata[j];
  }

  /**
   * Returns true when cell at {@code i} may legally claim position {@code j} this tick.
   *
   * <p>Two conditions must hold:
   * <ol>
   *   <li>Density: the moving cell is denser than whatever currently sits at {@code j}.</li>
   *   <li>No prior claim: {@code next[j]} is either empty or still holds the same type as
   *       {@code current[j]} — i.e., no other cell has already moved there this tick.</li>
   * </ol>
   * Without (2) two cells can both "win" the same destination, causing one to silently vanish.
   */
  private boolean canMoveTo(CellType cell, int j) {
    return densityMap.get(cell) > densityMap.get(current[j])
        && (next[j] == EMPTY || next[j] == current[j]);
  }

  /**
   * Gravity for granular solids (SAND, ASH): falls straight down then diagonally,
   * creating natural pile slopes.
   */
  private void applyGravity(CellType cell, int i, int x, int y) {
    if (inBounds(x, y + 1)) {
      int j = idx(x, y + 1);
      if (canMoveTo(cell, j)) {
        write(i, j);
        return;
      }
    }
    int dx1 = (tick & 1) == 0 ? -1 : 1;
    int dx2 = (tick & 1) == 0 ? 1 : -1;
    if (inBounds(x + dx1, y + 1)) {
      int j = idx(x + dx1, y + 1);
      if (canMoveTo(cell, j)) {
        write(i, j);
        return;
      }
    }
    if (inBounds(x + dx2, y + 1)) {
      int j = idx(x + dx2, y + 1);
      if (canMoveTo(cell, j)) {
        write(i, j);
        return;
      }
    }
    next[i] = cell;
    nextMetadata[i] = metadata[i];
  }

  /**
   * Gravity for liquids (WATER, OIL, ACID): falls straight down only — no diagonals.
   * Liquids pool flat rather than forming slopes. Horizontal spreading is handled
   * separately by {@link #moveToSides}.
   */
  private void liquidFall(CellType cell, int i, int x, int y) {
    if (inBounds(x, y + 1)) {
      int j = idx(x, y + 1);
      if (current[j] == FIRE && cell == WATER) {
        // Water douses fire below — water takes the cell, fire vanishes
        next[j] = WATER;
        nextMetadata[j] = 0;
        next[i] = EMPTY;
        return;
      }
      if (canMoveTo(cell, j)) {
        write(i, j);
        return;
      }
    }
    next[i] = cell;
    nextMetadata[i] = metadata[i];
  }

  /**
   * Horizontal spreading for liquids. Alternates preferred direction each tick.
   */
  private void moveToSides(CellType cell, int i, int x, int y) {
    int dx1 = (tick & 1) == 0 ? -1 : 1;
    int dx2 = (tick & 1) == 0 ? 1 : -1;
    for (int dx : new int[]{dx1, dx2}) {
      int nx = x + dx;
      if (!inBounds(nx, y)) {
        continue;
      }
      int j = idx(nx, y);
      if (current[j] == FIRE && cell == WATER) {
        // Water flowing sideways douses fire
        next[j] = WATER;
        nextMetadata[j] = 0;
        next[i] = EMPTY;
        return;
      }
      if (canMoveTo(cell, j)) {
        write(i, j);
        return;
      }
    }
  }

  /**
   * Applies oil movement rules for the cell at {@code (x, y)}.
   * Same horizontal-flow rules as water but lower density — floats above water.
   */
  private void oilIgnition(int i, int x, int y) {
    if (!current[i].equals(OIL)) {
      return;
    }
    for (int k = 0; k < neighX.length; k++) {
      int nx = x + neighX[k];
      int ny = y + neighY[k];
      if (inBounds(nx, ny)) {
        int j = idx(nx, ny);
        if (current[j].equals(FIRE)) {
          next[i] = FIRE;
          nextMetadata[i] = FIRE_TTL;
          return;
        }
      }
    }
  }


  /**
   * Applies fire rules for the cell at {@code (x, y)}.
   *
   * <p>Behaviour:
   * <ul>
   *   <li>Decrement {@code metadata[idx]} (remaining lifetime).</li>
   *   <li>At zero lifetime: cell becomes ASH.</li>
   *   <li>While alive: attempt to move upward into EMPTY; spread to adjacent
   *       flammable neighbours (OIL) with a certain probability.</li>
   * </ul>
   * Fire TTL and ignition probability are good candidates for constants at the top of this class.
   */


  private void fireSpread(int x, int y) {
    int i = idx(x, y);
    if (metadata[i] == 0) {
      next[i] = ASH;
      return;
    }

    // Water extinguishes fire immediately
    for (int k = 0; k < neighX.length; k++) {
      int nx = x + neighX[k];
      int ny = y + neighY[k];
      if (!inBounds(nx, ny)) {
        continue;
      }
      if (current[idx(nx, ny)] == WATER) {
        next[i] = EMPTY; // doused — no ash, just gone
        return;
      }
    }

    // Fire stays in place — it is a stationary flame, not a moving particle.
    // Growth happens by igniting combustible neighbours; smoke rises above it.
    next[i] = FIRE;
    nextMetadata[i] = metadata[i] - 1;

    // Spread to combustible neighbours
    for (int k = 0; k < neighX.length; k++) {
      int nx = x + neighX[k];
      int ny = y + neighY[k];
      if (!inBounds(nx, ny)) {
        continue;
      }
      int j = idx(nx, ny);
      if (isCombustible(current[j]) && rng.nextFloat() < IGNITE_CHANCE) {
        next[j] = FIRE;
        nextMetadata[j] = FIRE_TTL;
      }
    }

    // Young/strong fire rises: spawn a child fire cell directly above
    // Probability and TTL taper as the flame weakens
    float ttlFraction = (float) metadata[i] / FIRE_TTL;
    if (ttlFraction > 0.35f && inBounds(x, y - 1)) {
      int upIdx = idx(x, y - 1);
      CellType above = current[upIdx];
      if ((above == EMPTY || above == SMOKE) && next[upIdx] == EMPTY
          && rng.nextFloat() < ttlFraction * 0.75f) {
        next[upIdx] = FIRE;
        nextMetadata[upIdx] = (int) (metadata[i] * 0.6f);
      }
    }

    // Emit smoke directly above — rate tapers as TTL falls
    float smokeRate = ttlFraction * SMOKE_SPAWN_RATE;
    if (inBounds(x, y - 1) && current[idx(x, y - 1)] == EMPTY
        && next[idx(x, y - 1)] == EMPTY && rng.nextFloat() < smokeRate) {
      int smokeIdx = idx(x, y - 1);
      next[smokeIdx] = SMOKE;
      nextMetadata[smokeIdx] = SMOKE_TTL_BASE + rng.nextInt(SMOKE_TTL_VAR);
    }
  }

  /**
   * Applies acid rules for the cell at {@code (x, y)}.
   * Same flow rules as water.  Additionally, each tick acid checks its 4 cardinal
   * neighbours: if a neighbour is STONE, it may dissolve it (→ EMPTY) with a
   * configurable probability.
   */
  private void acidDissolve(int x, int y) {
    for (int k = 0; k < neighX.length; k++) {
      int nx = x + neighX[k];
      int ny = y + neighY[k];
      int i = idx(x, y);
      if (!inBounds(nx, ny)) {
        continue;
      }
      int j = idx(nx, ny);
      if (isDissolvable(current[j])) {
        if (rng.nextFloat() < DISSOLVE_CHANCE) {
          next[j] = EMPTY;
          // Acid fume: spawn short-lived smoke above the dissolution site
          int fumeY = y - 1;
          if (inBounds(x, fumeY) && current[idx(x, fumeY)] == EMPTY) {
            int fumeIdx = idx(x, fumeY);
            next[fumeIdx] = SMOKE;
            nextMetadata[fumeIdx] = ACID_FUME_TTL;
          }
        }
      }
    }
  }

  /**
   * Smoke rises upward and dissipates as its TTL counts down.
   * Spawn rate from fire tapers with fire TTL, so thick smoke appears when burning
   * strongly and thins to nothing as the fire dies.
   */
  private void smokeRise(int x, int y) {
    int i = idx(x, y);
    int ttl = metadata[i];
    if (ttl <= 0) {
      return; // dissipates — next[i] stays EMPTY (filled by prepareNextBuffer)
    }
    // Alternate left/right diagonal bias each tick to avoid one-sided drift
    int dx1 = (tick & 1) == 0 ? -1 : 1;
    int dx2 = (tick & 1) == 0 ? 1 : -1;
    int[] tryOffsets = {0, dx1, dx2}; // prefer straight up, then diagonals
    for (int dxOff : tryOffsets) {
      int ux = x + dxOff;
      int uy = y - 1;
      if (inBounds(ux, uy)) {
        int j = idx(ux, uy);
        if (current[j] == EMPTY && next[j] == EMPTY) {
          next[j] = SMOKE;
          nextMetadata[j] = ttl - 1;
          return; // smoke moved up; i stays EMPTY
        }
      }
    }
    // Blocked — stay and keep fading
    next[i] = SMOKE;
    nextMetadata[i] = ttl - 1;
  }
}
