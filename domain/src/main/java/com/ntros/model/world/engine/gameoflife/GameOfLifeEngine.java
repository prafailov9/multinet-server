package com.ntros.model.world.engine.gameoflife;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.Player;
import com.ntros.model.entity.movement.cell.Position;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.engine.solid.AbstractGridEngine;
import com.ntros.model.world.protocol.TileType;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.state.GridState;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * Conway's Game of Life engine.
 *
 * <h3>Cell representation</h3>
 * Live cells are stored as {@link TileType#ALIVE} in the world's terrain map.
 * Dead cells are {@link TileType#EMPTY}. All other tile types (WALL, WATER, …) act as
 * permanent obstacles — they never become alive and are never killed.
 *
 * <h3>Tick contract</h3>
 * {@link #applyIntents(GridState)} is called once per clock tick by the actor thread and
 * advances the simulation by exactly one generation according to Conway's rules:
 * <ul>
 *   <li>A live cell with 2 or 3 live neighbours survives.</li>
 *   <li>A dead cell with exactly 3 live neighbours becomes alive.</li>
 *   <li>All other cells die or remain dead.</li>
 * </ul>
 * The computation is done on a snapshot of the current state so that all cells in the
 * new generation are determined simultaneously — there are no ordering artefacts.
 *
 * <h3>Player joins</h3>
 * Connecting clients are registered as <em>spectators</em>: they appear in the entities
 * map so that leave / cleanup works correctly, but they are NOT placed in
 * {@code takenPositions} and do not block any GoL cell from changing state.
 */
@Slf4j
public class GameOfLifeEngine extends AbstractGridEngine {

  // ── Pre-allocated bitset buffers (sized lazily on first tick) ─────────────
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
  // staticMask   : boolean[] — true if the cell holds a permanent obstacle tile.
  //   Built once per world. Guards against accidental birth on a WALL/TRAP cell.
  //
  private long[] curr, next;
  private int[] votesBuf, dirtyBuf;
  private boolean[] staticMask;
  private int dirtyCount;
  private int bufW, bufH, rowLongs;

  /**
   * Allocates (or re-uses) the engine's working buffers for a world of the given size.
   * Called at the start of every tick; the inner check is a single int comparison,
   * so it adds no measurable overhead once sized.
   */
  private void ensureBuffers(int W, int H, Map<Vector4, TileType> terrain) {
    if (curr != null && bufW == W && bufH == H) {
      return;
    }
    rowLongs = (W + 63) >>> 6;
    int words = H * rowLongs;
    curr = new long[words];
    next = new long[words];
    votesBuf = new int[W * H];
    dirtyBuf = new int[W * H];
    staticMask = new boolean[W * H];
    bufW = W;
    bufH = H;

    // Mark permanently-blocked cells so we never birth on top of them.
    // For blank GoL worlds this loop is a no-op (terrain is empty at init time).
    for (Map.Entry<Vector4, TileType> e : terrain.entrySet()) {
      if (isStatic(e.getValue())) {
        int x = (int) e.getKey().getX();
        int y = (int) e.getKey().getY();
        if (x >= 0 && x < W && y >= 0 && y < H) {
          staticMask[y * W + x] = true;
        }
      }
    }
    log.debug("[GoL] Allocated bitset buffers for {}×{} world ({} KB).",
        W, H, (words * 8 + W * H * 5) / 1024);
  }


  @Override
  public void applyIntents(GridState state) {
    nextGeneration(state);
    state.moveIntents().clear(); // GoL has no player-driven move intents
  }

  /**
   * Handles an orchestrator command against the live terrain.
   * Runs on the actor thread — terrain mutations are safe without additional locking.
   */
  @Override
  public WorldResult orchestrate(OrchestrateRequest req, GridState state) {
    return switch (req.action()) {
      case SEED -> applySeed(req, state);
      case RANDOM_SEED -> applyRandomSeed(req.density(), state);
      case TOGGLE -> applyToggle(req, state);
      case CLEAR -> applyClear(state);
    };
  }

  /**
   * Registers the joining player as a spectator.
   * They are added to the entities map for tracking but do NOT occupy a GoL tile.
   */
  @Override
  public WorldResult joinEntity(JoinRequest req, GridState state) {
    if (state.entities().containsKey(req.playerName())) {
      return WorldResult.failed(req.playerName(), state.worldName(),
          "Spectator already registered: " + req.playerName());
    }
    long id = IdSequenceGenerator.getInstance().nextPlayerEntityId();
    Player spectator = new Player(Position.of(0, 0), req.playerName(), id, 0);
    state.entities().put(req.playerName(), spectator);
    log.info("[GoL] Spectator joined: '{}'.", req.playerName());
    return WorldResult.succeeded(req.playerName(), state.worldName(),
        "Joined GoL simulation as spectator.");
  }

  /**
   * Removes the spectator (no tile cleanup needed since they were never in takenPositions).
   */
  @Override
  public Entity removeEntity(String entityId, GridState state) {
    Entity removed = state.entities().remove(entityId);
    if (removed != null) {
      log.info("[GoL] Spectator removed: '{}'.", entityId);
    }
    return removed;
  }

  /**
   * Movement is not meaningful in a GoL simulation.
   * Clients that send MOVE to a GoL world receive an informative failure result.
   */
  @Override
  public WorldResult storeMoveIntent(MoveRequest req, GridState state) {
    log.warn("[GoL] MOVE rejected for '{}' — movement not supported in Game of Life.",
        req.playerId());
    return WorldResult.failed(req.playerId(), state.worldName(),
        "MOVE not supported in Game of Life. Use ORCHESTRATE to modify cell state.");
  }

  @Override
  public String serialize(GridState state) {
    return buildJson(state, false);
  }

  @Override
  public String serializeOneLine(GridState state) {
    return buildJson(state, true);
  }

  @Override
  public void reset(GridState state) {
    // Sparse: remove alive cells instead of overwriting all with EMPTY.
    state.terrain().entrySet().removeIf(e -> !isStatic(e.getValue()));
    state.entities().clear();
    state.moveIntents().clear();
    log.info("[GoL] World reset — all cells cleared, spectators removed.");
  }

  // ── Conway's rules ───────────────────────────────────────────────────────

  /**
   * Advances the terrain by one generation using a bitset double-buffer + flat vote array.
   *
   * <h3>Why this is fast</h3>
   * <ul>
   *   <li><b>Zero allocation in the hot path.</b> All working buffers ({@code curr}, {@code next},
   *       {@code votesBuf}, {@code dirtyBuf}) are pre-allocated engine fields reused across ticks.
   *       The old approach created ~2.5 M {@link Vector4} objects per tick (one per vote cast)
   *       and a {@code HashMap} to accumulate them.</li>
   *   <li><b>Cache-sequential access.</b> {@code curr}/{@code next} are contiguous {@code long[]}
   *       arrays; iterating them with the NTZL (numberOfTrailingZeros) bit-scan trick reads memory
   *       in order, giving near-perfect L2/L3 cache utilisation.  The old HashMap pointer-chases
   *       scattered across the heap caused cache misses on virtually every entry.</li>
   *   <li><b>O(alive) with tiny constant.</b> Complexity is the same as before (sparse voting),
   *       but the constant is ~10–50× smaller: array indexing vs HashMap.merge + equals/hashCode
   *       on {@link Vector4}.</li>
   *   <li><b>Near-zero steady-state cost.</b> Step 4 diffs the two bitsets word-by-word;
   *       words that didn't change are skipped in one branch.  When the pattern stabilises,
   *       almost no HashMap mutations happen.</li>
   * </ul>
   *
   * <h3>Algorithm</h3>
   * <ol>
   *   <li><b>Load terrain → {@code curr}.</b>  Scan terrain HashMap for ALIVE entries; set
   *       the corresponding bit in {@code curr}.  O(alive).</li>
   *   <li><b>Vote.</b>  NTZL-scan {@code curr}; each alive cell increments {@code votesBuf} for
   *       its 8 Moore neighbours and records the index in {@code dirtyBuf} on first write.
   *       O(alive × 8).</li>
   *   <li><b>Determine {@code next}.</b>  Iterate only {@code dirtyBuf} entries; apply Conway's
   *       rules; clear {@code votesBuf} inline (no O(W×H) fill needed).  Alive cells with 0
   *       votes never appear in {@code dirtyBuf} → never set in {@code next} → die naturally.
   *       O(dirty ≤ alive × 8).</li>
   *   <li><b>Diff → update terrain.</b>  Compare {@code curr}/{@code next} word-by-word;
   *       for changed bits remove (deaths) or put (births) from/into the terrain HashMap.
   *       O(cells.length = W×H/64).</li>
   * </ol>
   */
  private void nextGeneration(GridState state) {
    Map<Vector4, TileType> terrain = state.terrain();
    final int W = state.dimension().getWidth();
    final int H = state.dimension().getHeight();
    ensureBuffers(W, H, terrain);

    // ── Step 1: build curr from terrain ──────────────────────────────────────
    Arrays.fill(curr, 0L);
    for (Map.Entry<Vector4, TileType> entry : terrain.entrySet()) {
      if (entry.getValue() != TileType.ALIVE) {
        continue;
      }
      int x = (int) entry.getKey().getX();
      int y = (int) entry.getKey().getY();
      curr[y * rowLongs + (x >>> 6)] |= 1L << (x & 63);
    }

    // ── Step 2: each alive cell votes to its 8 Moore neighbours ──────────────
    // votesBuf[ny*W+nx] counts how many alive neighbours (nx,ny) has.
    // dirtyBuf records each index the first time it is written (votesBuf was 0).
    // This lets us clear exactly the written slots in step 3 without an O(W×H) fill.
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

    // ── Step 3: determine next generation ────────────────────────────────────
    // next starts all-zero.  We only set bits for cells that survive or are born.
    // Alive cells with 0 votes never appear in dirtyBuf → not set in next → die.
    Arrays.fill(next, 0L);
    for (int i = 0; i < dirtyCount; i++) {
      final int idx = dirtyBuf[i];
      final int n = votesBuf[idx];
      votesBuf[idx] = 0; // clear inline — avoids a separate O(W×H) fill pass
      final int x = idx % W;
      final int y = idx / W;
      final boolean alive = (curr[y * rowLongs + (x >>> 6)] >>> (x & 63) & 1L) != 0;
      final boolean survives = alive ? (n == 2 || n == 3) : (n == 3 && !staticMask[idx]);
      if (survives) {
        next[y * rowLongs + (x >>> 6)] |= 1L << (x & 63);
      }
    }

    // ── Step 4: diff curr / next → update terrain ────────────────────────────
    // Only cells that actually changed are touched.
    // In steady-state (stable / oscillating patterns) most words are identical → near-zero work.
    for (int wi = 0; wi < curr.length; wi++) {
      final long died = curr[wi] & ~next[wi]; // was alive, now dead
      final long born = next[wi] & ~curr[wi]; // was dead, now alive
      if ((died | born) == 0L) {
        continue;
      }
      final int row = wi / rowLongs;
      final int colBase = (wi % rowLongs) << 6;
      long d = died;
      while (d != 0L) {
        final int bit = Long.numberOfTrailingZeros(d);
        d &= d - 1;
        final int x = colBase + bit;
        if (x < W) {
          terrain.remove(Vector4.of(x, row, 0, 0));
        }
      }
      long b = born;
      while (b != 0L) {
        final int bit = Long.numberOfTrailingZeros(b);
        b &= b - 1;
        final int x = colBase + bit;
        if (x < W) {
          terrain.put(Vector4.of(x, row, 0, 0), TileType.ALIVE);
        }
      }
    }
  }

  /**
   * Returns {@code true} for tiles that participate in GoL rules.
   */
  private static boolean isStatic(TileType tile) {
    return tile != TileType.EMPTY && tile != TileType.ALIVE;
  }

  // ── Orchestrate actions ──────────────────────────────────────────────────

  private WorldResult applySeed(OrchestrateRequest req, GridState state) {
    if (req.cells() == null || req.cells().isEmpty()) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "SEED requires at least one cell position.");
    }
    int set = 0;
    for (Position cell : req.cells()) {
      Vector4 pos = Vector4.of(cell.getX(), cell.getY(), 0, 0);
      if (state.isWithinBounds(pos)) {
        state.terrain().put(pos, TileType.ALIVE);
        set++;
      } else {
        log.warn("[GoL] SEED: position {} is out of bounds — skipped.", cell);
      }
    }
    log.info("[GoL] SEED: {} cells made alive.", set);
    return WorldResult.succeeded("orchestrator", state.worldName(),
        "SEED: " + set + " cells set alive.");
  }

  private WorldResult applyRandomSeed(float density, GridState state) {
    if (density < 0f || density > 1f) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "RANDOM_SEED density must be in [0.0, 1.0], got: " + density);
    }
    int width = state.dimension().getWidth();
    int height = state.dimension().getHeight();
    int alive = 0;

    // Sparse: remove existing non-static cells first, then only insert ALIVE ones.
    state.terrain().entrySet().removeIf(e -> !isStatic(e.getValue()));

    ThreadLocalRandom rng = ThreadLocalRandom.current();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (rng.nextDouble() < density) {
          Vector4 pos = Vector4.of(x, y, 0, 0);
          // Skip positions that hold a static obstacle (WALL, WATER, TRAP, …)
          if (!isStatic(state.terrain().getOrDefault(pos, TileType.EMPTY))) {
            state.terrain().put(pos, TileType.ALIVE);
            alive++;
          }
        }
      }
    }
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
        TileType current = state.terrain().getOrDefault(pos, TileType.EMPTY);
        if (!isStatic(current)) {
          // Sparse: remove the entry for dead cells instead of writing EMPTY
          if (current == TileType.ALIVE) {
            state.terrain().remove(pos);
          } else {
            state.terrain().put(pos, TileType.ALIVE);
          }
          toggled++;
        }
      } else {
        log.warn("[GoL] TOGGLE: position {} is out of bounds — skipped.", cell);
      }
    }
    log.info("[GoL] TOGGLE: {} cells flipped.", toggled);
    return WorldResult.succeeded("orchestrator", state.worldName(),
        "TOGGLE: " + toggled + " cells flipped.");
  }

  private WorldResult applyClear(GridState state) {
    // Sparse: remove alive cells instead of overwriting all with EMPTY
    state.terrain().entrySet().removeIf(e -> !isStatic(e.getValue()));
    log.info("[GoL] CLEAR: all live cells removed.");
    return WorldResult.succeeded("orchestrator", state.worldName(), "CLEAR: simulation reset.");
  }

  // ── Serialisation helpers ────────────────────────────────────────────────

  private String buildJson(GridState state, boolean oneLine) {
    String sep = oneLine ? "" : "\n";
    String indent = oneLine ? "" : "  ";
    StringBuilder sb = new StringBuilder("{").append(sep);

    sb.append(indent).append("\"tiles\": {").append(sep);
    var tileIt = state.terrain().entrySet().iterator();
    while (tileIt.hasNext()) {
      var entry = tileIt.next();
      Vector4 p = entry.getKey();
      sb.append(indent).append(indent)
          .append("\"").append((int) p.getX()).append(",").append((int) p.getY()).append("\": ")
          .append("\"").append(entry.getValue().name()).append("\"");
      if (tileIt.hasNext()) {
        sb.append(",");
      }
      sb.append(sep);
    }
    sb.append(indent).append("},").append(sep);

    sb.append(indent).append("\"entities\": {").append(sep);
    var entIt = state.entities().entrySet().iterator();
    while (entIt.hasNext()) {
      var entry = entIt.next();
      Position pos = entry.getValue().getPosition();
      sb.append(indent).append(indent)
          .append("\"").append(entry.getKey()).append("\": ")
          .append("{\"x\":").append(pos.getX()).append(",\"y\":").append(pos.getY()).append("}");
      if (entIt.hasNext()) {
        sb.append(",");
      }
      sb.append(sep);
    }
    sb.append(indent).append("}").append(sep).append("}");

    return sb.toString();
  }
}
