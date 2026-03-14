package com.ntros.model.world.engine.d2.grid.gameoflife;

import com.ntros.model.entity.movement.grid.Position;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.engine.core.SimulationGridEngine;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.core.SimulationGridState;
import com.ntros.model.world.state.grid.CellType;
import com.ntros.model.world.state.grid.GridSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * Conway's Game of Life engine.
 *
 * <h3>Cell representation</h3>
 * The engine owns the authoritative alive-cell state as a pre-allocated {@code long[]} bitset
 * ({@code curr}).  Dead cells are implicitly represented by a cleared bit.  There is no terrain
 * map dependency — {@link SimulationGridState} intentionally exposes none.
 *
 * <h3>Tick contract</h3>
 * {@link #applyIntents(GridState)} is called once per clock tick by the actor thread and
 * advances the simulation by exactly one generation according to Conway's rules:
 * <ul>
 *   <li>A live cell with 2 or 3 live neighbours survives.</li>
 *   <li>A dead cell with exactly 3 live neighbours becomes alive.</li>
 *   <li>All other cells die or remain dead.</li>
 * </ul>
 *
 * <h3>Diff-based broadcasts</h3>
 * {@link #snapshot(GridState)} returns a {@link GridSnapshot} (full state) whenever the world
 * was freshly seeded or reset, and a compact {@link GridDiff} on every subsequent call.
 * The diff is computed against {@code broadcastBasis} — the bitset as it was at the time of the
 * previous broadcast — so that skipped ticks (due to broadcast rate-limiting) are accumulated
 * correctly rather than silently lost.
 */
@Slf4j
public class GameOfLifeEngine implements SimulationGridEngine {

  // ── Pre-allocated bitset buffers (sized lazily on first use) ──────────────
  //
  // curr / next  : one bit per cell — set = ALIVE, clear = DEAD.
  //   Layout: word index = y * rowLongs + (x >>> 6), bit = x & 63.
  //   Size for gol-big (1024×1024): 16 384 longs = 128 KB — fits in L2 cache.
  //
  // votesBuf     : int per cell — alive-neighbour count cast during voting.
  //   Only entries listed in dirtyBuf are non-zero; we clear them inline,
  //   so we never pay the O(W×H) Arrays.fill cost.
  //
  // dirtyBuf     : int[] of flat indices (y*W+x) that received at least one vote.
  //   Capped at W×H entries (4 MB for gol-big); typical usage << W×H.
  //
  // broadcastBasis : snapshot of the alive-cell bitset as of the previous broadcast.
  //   Used by snapshot() to compute the diff since the last time data was sent to
  //   clients, regardless of how many simulation ticks were skipped in between.
  //
  private long[] curr, next, broadcastBasis;
  private int[] votesBuf, dirtyBuf;
  private int dirtyCount;
  private int bufW, bufH, rowLongs;

  /**
   * When true, {@link #snapshot} returns a full {@link GridSnapshot} and resets the
   * broadcast basis.  Set after any state-altering orchestrate action or reset.
   */
  private boolean needsFullSnapshot = true;

  // ── SimulationGridEngine API ───────────────────────────────────────────────

  /**
   * Handles an orchestrator command, writing directly into the {@code curr} bitset.
   * Runs on the actor thread — buffer mutations are safe without additional locking.
   */
  @Override
  public WorldResult orchestrate(OrchestrateRequest req, SimulationGridState state) {
    ensureBuffers(state.dimension().getWidth(), state.dimension().getHeight());
    return switch (req.action()) {
      case SEED -> applySeed(req, state);
      case RANDOM_SEED -> applyRandomSeed(req.density(), state);
      case TOGGLE -> applyToggle(req, state);
      case CLEAR -> applyClear(state);
      default -> WorldResult.failed("orchestrator", state.worldName(),
          "GoL world only supports SEED, RANDOM_SEED, TOGGLE, CLEAR orchestration.");
    };
  }

  @Override
  public void applyIntents(GridState state) {
    ensureBuffers(state.dimension().getWidth(), state.dimension().getHeight());
    nextGeneration();
  }

  /**
   * Returns either a full {@link GridSnapshot} or a compact {@link GridDiff}, depending on
   * whether the world was recently seeded / reset.
   *
   * <p>Only called by the actor when a broadcast will actually go out this tick — updating
   * {@code broadcastBasis} here is therefore safe; we only advance the baseline when data is
   * actually sent to clients.
   */
  @Override
  public Object snapshot(GridState state) {
    if (curr == null) {
      // Buffers not yet initialised — world has never ticked and was never orchestrated
      return new GridSnapshot(Map.of(), Map.of());
    }

    if (needsFullSnapshot) {
      needsFullSnapshot = false;
      System.arraycopy(curr, 0, broadcastBasis, 0, curr.length);
      return new GridSnapshot(buildTerrainFromCurr(), Map.of());
    }

    // ── Compute diff from broadcastBasis → curr ───────────────────────────────
    // Any tick that was skipped (broadcast rate-limiting) is accumulated: a cell
    // that was born, died, and born again between two broadcasts shows up as
    // "born" only — the XOR across all skipped words captures the net change.
    List<Vector4> bornList = new ArrayList<>();
    List<Vector4> diedList = new ArrayList<>();
    final int W = bufW;
    for (int wi = 0; wi < curr.length; wi++) {
      long changed = broadcastBasis[wi] ^ curr[wi];
      if (changed == 0L) {
        continue;
      }
      final int row = wi / rowLongs;
      final int colBase = (wi % rowLongs) << 6;
      long born = changed & curr[wi];
      while (born != 0L) {
        final int bit = Long.numberOfTrailingZeros(born);
        born &= born - 1;
        final int x = colBase + bit;
        if (x < W) {
          bornList.add(Vector4.of(x, row, 0, 0));
        }
      }
      long died = changed & ~curr[wi];
      while (died != 0L) {
        final int bit = Long.numberOfTrailingZeros(died);
        died &= died - 1;
        final int x = colBase + bit;
        if (x < W) {
          diedList.add(Vector4.of(x, row, 0, 0));
        }
      }
    }
    System.arraycopy(curr, 0, broadcastBasis, 0, curr.length);
    return new GridDiff(bornList, diedList, Map.of());
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
  public void reset(GridState state) {
    if (curr != null) {
      Arrays.fill(curr, 0L);
    }
    needsFullSnapshot = true;
    log.info("[GoL] World reset — all cells cleared.");
  }

  // ── Bitset buffer management ───────────────────────────────────────────────

  /**
   * Allocates (or re-uses) the engine's working buffers for a world of the given size.
   * Called at the start of every tick and every orchestrate; the inner check is a single
   * comparison, so it adds no measurable overhead once sized.
   *
   * <pre>
   * Example: 256×256
   *   rowLongs = ceil(256/64) = 4
   *   words    = 256 × 4     = 1 024
   *   array size             = 1 024 longs
   *   each long holds 64 cells: 1 024 × 64 = 65 536 = 256 × 256 ✓
   * </pre>
   */
  private void ensureBuffers(int W, int H) {
    if (curr != null && bufW == W && bufH == H) {
      return;
    }
    rowLongs = (W + 63) >>> 6;
    int words = H * rowLongs;
    curr = new long[words];
    next = new long[words];
    broadcastBasis = new long[words];
    votesBuf = new int[W * H];
    dirtyBuf = new int[W * H];
    bufW = W;
    bufH = H;
    needsFullSnapshot = true;
    log.debug("[GoL] Allocated bitset buffers for {}×{} world ({} KB).",
        W, H, (words * 8 + W * H * 4) / 1024);
  }

  // ── Conway's rules ───────────────────────────────────────────────────────

  /**
   * Advances the simulation by one generation using a bitset double-buffer + flat vote array.
   *
   * <h3>Why this is fast</h3>
   * <ul>
   *   <li><b>Zero allocation in the hot path.</b> All working buffers are pre-allocated engine
   *       fields reused across ticks.</li>
   *   <li><b>Cache-sequential access.</b> {@code curr}/{@code next} are contiguous {@code long[]}
   *       arrays; the NTZL bit-scan reads memory in order, giving near-perfect L2 utilisation.</li>
   *   <li><b>O(alive) with tiny constant.</b> Voting scans only non-zero words of {@code curr}.
   *       </li>
   *   <li><b>Buffer swap.</b> After step 3, {@code curr} and {@code next} are swapped — no
   *       allocation, no extra fill.  Orchestrate writes directly into whichever array is
   *       currently {@code curr}.</li>
   * </ul>
   *
   * <h3>Algorithm</h3>
   * <ol>
   *   <li><b>Vote.</b> NTZL-scan {@code curr}; each alive cell increments {@code votesBuf} for
   *       its 8 Moore neighbours and records the index in {@code dirtyBuf} on first write.</li>
   *   <li><b>Determine {@code next}.</b> Iterate only {@code dirtyBuf} entries; apply Conway's
   *       rules; clear {@code votesBuf} inline.</li>
   *   <li><b>Swap buffers.</b> {@code curr} ↔ {@code next} — no allocation, no extra fill.</li>
   * </ol>
   */
  private void nextGeneration() {
    final int W = bufW;
    final int H = bufH;

    // ── Step 1: each alive cell votes to its 8 Moore neighbours ──────────────
    dirtyCount = 0;
    for (int wi = 0; wi < curr.length; wi++) {
      long word = curr[wi];
      if (word == 0L) {
        continue;
      }
      final int row = wi / rowLongs;
      final int colBase = (wi % rowLongs) << 6;
      while (word != 0L) {
        final int bit = Long.numberOfTrailingZeros(word);
        final int x = colBase + bit;
        word &= word - 1;
        if (x >= W) {
          continue; // skip padding bits in the last long of a row
        }
        for (int dy = -1; dy <= 1; dy++) {
          final int ny = row + dy;
          if (ny < 0 || ny >= H) {
            continue;
          }
          for (int dx = -1; dx <= 1; dx++) {
            if (dx == 0 && dy == 0) {
              continue;
            }
            final int nx = x + dx;
            if (nx < 0 || nx >= W) {
              continue;
            }
            final int idx = ny * W + nx;
            if (votesBuf[idx] == 0) {
              dirtyBuf[dirtyCount++] = idx;
            }
            votesBuf[idx]++;
          }
        }
      }
    }

    // ── Step 2: determine next generation ────────────────────────────────────
    Arrays.fill(next, 0L);
    for (int i = 0; i < dirtyCount; i++) {
      final int idx = dirtyBuf[i];
      final int n = votesBuf[idx];
      votesBuf[idx] = 0; // clear inline — avoids a separate O(W×H) fill pass
      final int x = idx % W;
      final int y = idx / W;
      final boolean alive = (curr[y * rowLongs + (x >>> 6)] >>> (x & 63) & 1L) != 0;
      final boolean survives = alive ? (n == 2 || n == 3) : (n == 3);
      if (survives) {
        next[y * rowLongs + (x >>> 6)] |= 1L << (x & 63);
      }
    }

    // ── Step 3: swap buffers ──────────────────────────────────────────────────
    long[] tmp = curr;
    curr = next;
    next = tmp;
  }

  // ── Orchestrate actions ───────────────────────────────────────────────────

  private WorldResult applySeed(OrchestrateRequest req, GridState state) {
    if (req.cells() == null || req.cells().isEmpty()) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "SEED requires at least one cell position.");
    }
    int set = 0;
    for (Position cell : req.cells()) {
      Vector4 pos = Vector4.of(cell.getX(), cell.getY(), 0, 0);
      if (state.isWithinBounds(pos)) {
        int x = cell.getX();
        int y = cell.getY();
        curr[y * rowLongs + (x >>> 6)] |= 1L << (x & 63);
        set++;
      } else {
        log.warn("[GoL] SEED: position {} is out of bounds — skipped.", cell);
      }
    }
    needsFullSnapshot = true;
    log.info("[GoL] SEED: {} cells made alive.", set);
    return WorldResult.succeeded("orchestrator", state.worldName(),
        "SEED: " + set + " cells set alive.");
  }

  private WorldResult applyRandomSeed(float density, GridState state) {
    if (density < 0f || density > 1f) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "RANDOM_SEED density must be in [0.0, 1.0], got: " + density);
    }
    int width = bufW;
    int height = bufH;
    Arrays.fill(curr, 0L);
    int alive = 0;
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (rng.nextDouble() < density) {
          curr[y * rowLongs + (x >>> 6)] |= 1L << (x & 63);
          alive++;
        }
      }
    }
    needsFullSnapshot = true;
    log.info("[GoL] RANDOM_SEED density={}: {} cells made alive.", density, alive);
    return WorldResult.succeeded("orchestrator", state.worldName(),
        "RANDOM_SEED: " + alive + " cells alive at density " + density);
  }

  private WorldResult applyToggle(OrchestrateRequest req, GridState state) {
    if (req.cells() == null || req.cells().isEmpty()) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "TOGGLE requires at least one cell position.");
    }
    int toggled = 0;
    for (Position cell : req.cells()) {
      Vector4 pos = Vector4.of(cell.getX(), cell.getY(), 0, 0);
      if (state.isWithinBounds(pos)) {
        int x = cell.getX();
        int y = cell.getY();
        int wi = y * rowLongs + (x >>> 6);
        long mask = 1L << (x & 63);
        if ((curr[wi] & mask) != 0) {
          curr[wi] &= ~mask;
        } else {
          curr[wi] |= mask;
        }
        toggled++;
      } else {
        log.warn("[GoL] TOGGLE: position {} is out of bounds — skipped.", cell);
      }
    }
    needsFullSnapshot = true;
    log.info("[GoL] TOGGLE: {} cells flipped.", toggled);
    return WorldResult.succeeded("orchestrator", state.worldName(),
        "TOGGLE: " + toggled + " cells flipped.");
  }

  private WorldResult applyClear(GridState state) {
    Arrays.fill(curr, 0L);
    needsFullSnapshot = true;
    log.info("[GoL] CLEAR: all live cells removed.");
    return WorldResult.succeeded("orchestrator", state.worldName(), "CLEAR: simulation reset.");
  }

  // ── Serialisation helpers ─────────────────────────────────────────────────

  /**
   * Builds a transient terrain map from {@code curr} for use in a full {@link GridSnapshot}.
   * Only called when {@code needsFullSnapshot} is true — typically once after seeding.
   */
  private Map<Vector4, CellType> buildTerrainFromCurr() {
    Map<Vector4, CellType> terrain = new LinkedHashMap<>();
    final int W = bufW;
    for (int wi = 0; wi < curr.length; wi++) {
      long word = curr[wi];
      if (word == 0L) {
        continue;
      }
      final int row = wi / rowLongs;
      final int colBase = (wi % rowLongs) << 6;
      while (word != 0L) {
        final int bit = Long.numberOfTrailingZeros(word);
        word &= word - 1;
        final int x = colBase + bit;
        if (x < W) {
          terrain.put(Vector4.of(x, row, 0, 0), CellType.ALIVE);
        }
      }
    }
    return terrain;
  }

  private String buildJson(boolean oneLine) {
    String sep = oneLine ? "" : "\n";
    String indent = oneLine ? "" : "  ";
    StringBuilder sb = new StringBuilder("{").append(sep);
    sb.append(indent).append("\"tiles\": {").append(sep);

    if (curr != null) {
      final int W = bufW;
      boolean firstTile = true;
      for (int wi = 0; wi < curr.length; wi++) {
        long word = curr[wi];
        if (word == 0L) {
          continue;
        }
        final int row = wi / rowLongs;
        final int colBase = (wi % rowLongs) << 6;
        while (word != 0L) {
          final int bit = Long.numberOfTrailingZeros(word);
          word &= word - 1;
          final int x = colBase + bit;
          if (x < W) {
            if (!firstTile) {
              sb.append(",").append(sep);
            }
            firstTile = false;
            sb.append(indent).append(indent)
                .append('"').append(x).append(',').append(row).append("\": \"ALIVE\"");
          }
        }
      }
    }

    sb.append(sep).append(indent).append("},").append(sep);
    sb.append(indent).append("\"entities\": {}").append(sep).append("}");
    return sb.toString();
  }
}
