package com.ntros.model.world.engine.gameoflife;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.Player;
import com.ntros.model.entity.movement.cell.Position;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.engine.solid.WorldEngine;
import com.ntros.model.world.protocol.TileType;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.state.GridState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
public class GameOfLifeEngine implements WorldEngine {

  // ── WorldEngine – tick ────────────────────────────────────────────────────

  @Override
  public void applyIntents(GridState state) {
    nextGeneration(state);
    state.moveIntents().clear(); // GoL has no player-driven move intents
  }

  // ── WorldEngine – orchestrate (GoL-specific) ──────────────────────────────

  /**
   * Handles an orchestrator command against the live terrain.
   * Runs on the actor thread — terrain mutations are safe without additional locking.
   */
  @Override
  public WorldResult orchestrate(OrchestrateRequest req, GridState state) {
    return switch (req.action()) {
      case SEED        -> applySeed(req, state);
      case RANDOM_SEED -> applyRandomSeed(req.density(), state);
      case TOGGLE      -> applyToggle(req, state);
      case CLEAR       -> applyClear(state);
    };
  }

  // ── WorldEngine – player lifecycle ───────────────────────────────────────

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
    log.warn("[GoL] MOVE rejected for '{}' — movement not supported in Game of Life.", req.playerId());
    return WorldResult.failed(req.playerId(), state.worldName(),
        "MOVE not supported in Game of Life. Use ORCHESTRATE to modify cell state.");
  }

  // ── WorldEngine – serialisation (delegates to shared helpers) ────────────

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
   * Advances the terrain by one generation using a sparse alive-cell voting algorithm.
   *
   * <p>Instead of iterating every cell in the W×H grid (O(W×H)), only alive cells iterate and
   * cast votes to their 8 Moore neighbours (O(alive × 8)). This is dramatically faster for
   * sparse or partially populated worlds (e.g. 1024×1024 with 30% density = 314k alive cells
   * → ~2.5M operations vs 8M for the naive approach, and improving as the board stabilises).
   *
   * <p>The terrain map is kept <em>sparse</em>: only {@link TileType#ALIVE} entries (and any
   * static obstacle tiles) are stored. EMPTY cells are simply absent from the map.
   */
  private void nextGeneration(GridState state) {
    Map<Vector4, TileType> terrain = state.terrain();
    int width  = state.dimension().getWidth();
    int height = state.dimension().getHeight();

    // ── Step 1: each alive cell casts a vote to each of its 8 neighbours ──────
    // votes[pos] = number of alive neighbours that pos has
    Map<Vector4, Integer> votes = new HashMap<>();
    for (Map.Entry<Vector4, TileType> entry : terrain.entrySet()) {
      if (entry.getValue() != TileType.ALIVE) continue;
      int x = (int) entry.getKey().getX();
      int y = (int) entry.getKey().getY();
      for (int dx = -1; dx <= 1; dx++) {
        for (int dy = -1; dy <= 1; dy++) {
          if (dx == 0 && dy == 0) continue;
          int nx = x + dx, ny = y + dy;
          if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
            votes.merge(Vector4.of(nx, ny, 0, 0), 1, Integer::sum);
          }
        }
      }
    }

    // ── Step 2: determine births and deaths from the vote map ──────────────────
    List<Vector4> toKill  = new ArrayList<>();
    List<Vector4> toBirth = new ArrayList<>();

    for (Map.Entry<Vector4, Integer> e : votes.entrySet()) {
      Vector4 pos   = e.getKey();
      int     n     = e.getValue();
      TileType tile = terrain.getOrDefault(pos, TileType.EMPTY);
      if (isStatic(tile)) continue;

      if (tile == TileType.ALIVE) {
        // Survival: 2 or 3 neighbours → stay alive; any other count → die
        if (n != 2 && n != 3) toKill.add(pos);
      } else {
        // Birth: exactly 3 neighbours
        if (n == 3) toBirth.add(pos);
      }
    }

    // Alive cells that received zero votes have no alive neighbours → they die
    for (Map.Entry<Vector4, TileType> e : terrain.entrySet()) {
      if (e.getValue() == TileType.ALIVE && !votes.containsKey(e.getKey())) {
        toKill.add(e.getKey());
      }
    }

    // ── Step 3: apply changes (sparse: remove dead, add born) ─────────────────
    for (Vector4 pos : toKill)  terrain.remove(pos);
    for (Vector4 pos : toBirth) terrain.put(pos, TileType.ALIVE);
  }

  /** Returns {@code true} for tiles that participate in GoL rules. */
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
    int width  = state.dimension().getWidth();
    int height = state.dimension().getHeight();
    int alive  = 0;

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
      if (tileIt.hasNext()) sb.append(",");
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
      if (entIt.hasNext()) sb.append(",");
      sb.append(sep);
    }
    sb.append(indent).append("}").append(sep).append("}");

    return sb.toString();
  }
}
