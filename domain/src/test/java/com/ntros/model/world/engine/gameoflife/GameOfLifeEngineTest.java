package com.ntros.model.world.engine.gameoflife;

import static org.assertj.core.api.Assertions.assertThat;

import com.ntros.model.entity.movement.grid.Position;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.engine.d2.grid.gameoflife.GameOfLifeEngine;
import com.ntros.model.world.protocol.request.OrchestrateAction;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.grid.CellType;
import com.ntros.model.world.state.grid.GameOfLifeState;
import com.ntros.model.world.state.grid.GridSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GameOfLifeEngine}.
 *
 * <p>After the interface-segregation refactor, {@code GameOfLifeEngine} is a
 * {@code SimulationGridEngine}; it owns its alive-cell state as an internal {@code long[]}
 * bitset and never reads or writes a terrain map on the state object.
 *
 * <p>All cell reads/writes in tests therefore go through the engine's own public API:
 * <ul>
 *   <li>{@code orchestrate(SEED, …)} to set cells alive before a tick.</li>
 *   <li>{@code applyIntents(state)} to advance one generation.</li>
 *   <li>{@code snapshot(state)} to materialise the current bitset into a {@link GridSnapshot}
 *       whose terrain map can then be asserted against.</li>
 * </ul>
 */
class GameOfLifeEngineTest {

  private static final String WORLD = "gol-test";

  private GameOfLifeEngine engine;
  private GameOfLifeState state;

  @BeforeEach
  void setUp() {
    engine = new GameOfLifeEngine();
    state = new GameOfLifeState(WORLD, 10, 10);
  }

  // ── Test helpers ──────────────────────────────────────────────────────────

  /** Seed one or more cells alive by calling ORCHESTRATE SEED. */
  private void seedCells(Position... positions) {
    engine.orchestrate(
        new OrchestrateRequest(OrchestrateAction.SEED, List.of(positions), 0f, ""),
        state);
  }

  /**
   * Advance the simulation by one tick and capture the resulting full terrain snapshot.
   *
   * <p>Because {@code orchestrate} always sets {@code needsFullSnapshot = true}, the first
   * call to {@code snapshot()} after a seed/tick pair returns a complete {@link GridSnapshot}
   * rather than a diff — so there is no need to call it twice.
   */
  private Map<Vector4, CellType> tickAndSnapshot() {
    engine.applyIntents(state);
    return ((GridSnapshot) engine.snapshot(state)).terrain();
  }

  /**
   * Materialise the current engine state without ticking.
   * Useful for verifying the result of an orchestrate action directly.
   */
  private Map<Vector4, CellType> currentSnapshot() {
    return ((GridSnapshot) engine.snapshot(state)).terrain();
  }

  private static Vector4 pos(int x, int y) {
    return Vector4.of(x, y, 0, 0);
  }

  // ── Conway survival rules ─────────────────────────────────────────────────

  @Nested
  class SurvivalRules {

    @Test
    void liveCell_with2Neighbours_survives() {
      seedCells(Position.of(4, 5), Position.of(5, 5), Position.of(6, 5));

      Map<Vector4, CellType> terrain = tickAndSnapshot();

      assertThat(terrain.getOrDefault(pos(5, 5), CellType.EMPTY)).isEqualTo(CellType.ALIVE);
    }

    @Test
    void liveCell_with3Neighbours_survives() {
      // L-shape: (5,5) has 3 neighbours
      seedCells(Position.of(4, 5), Position.of(5, 5), Position.of(6, 5), Position.of(5, 6));

      Map<Vector4, CellType> terrain = tickAndSnapshot();

      assertThat(terrain.getOrDefault(pos(5, 5), CellType.EMPTY)).isEqualTo(CellType.ALIVE);
    }

    @Test
    void liveCell_with1Neighbour_dies() {
      seedCells(Position.of(4, 5), Position.of(5, 5));

      Map<Vector4, CellType> terrain = tickAndSnapshot();

      assertThat(terrain.getOrDefault(pos(5, 5), CellType.EMPTY)).isEqualTo(CellType.EMPTY);
    }

    @Test
    void liveCell_with4Neighbours_diesByOvercrowding() {
      // (5,5) has 4 diagonal neighbours
      seedCells(
          Position.of(4, 4), Position.of(6, 4),
          Position.of(4, 6), Position.of(6, 6),
          Position.of(5, 5));

      Map<Vector4, CellType> terrain = tickAndSnapshot();

      assertThat(terrain.getOrDefault(pos(5, 5), CellType.EMPTY)).isEqualTo(CellType.EMPTY);
    }

    @Test
    void liveCell_with0Neighbours_dies() {
      seedCells(Position.of(5, 5));

      Map<Vector4, CellType> terrain = tickAndSnapshot();

      assertThat(terrain.getOrDefault(pos(5, 5), CellType.EMPTY)).isEqualTo(CellType.EMPTY);
    }
  }

  // ── Conway birth rule ─────────────────────────────────────────────────────

  @Nested
  class BirthRule {

    @Test
    void deadCell_with3Neighbours_becomesAlive() {
      // Three cells around (5,5) — (5,5) itself is dead
      seedCells(Position.of(4, 5), Position.of(5, 4), Position.of(6, 5));

      Map<Vector4, CellType> terrain = tickAndSnapshot();

      assertThat(terrain.getOrDefault(pos(5, 5), CellType.EMPTY)).isEqualTo(CellType.ALIVE);
    }

    @Test
    void deadCell_with2Neighbours_staysDead() {
      seedCells(Position.of(4, 5), Position.of(6, 5));

      Map<Vector4, CellType> terrain = tickAndSnapshot();

      assertThat(terrain.getOrDefault(pos(5, 5), CellType.EMPTY)).isEqualTo(CellType.EMPTY);
    }

    @Test
    void deadCell_with4Neighbours_staysDead() {
      seedCells(
          Position.of(4, 4), Position.of(6, 4),
          Position.of(4, 6), Position.of(6, 6));

      Map<Vector4, CellType> terrain = tickAndSnapshot();

      assertThat(terrain.getOrDefault(pos(5, 5), CellType.EMPTY)).isEqualTo(CellType.EMPTY);
    }
  }

  // ── Stable patterns ───────────────────────────────────────────────────────

  @Nested
  class StablePatterns {

    /**
     * Blinker: three cells in a vertical column oscillates to a horizontal row after one tick.
     */
    @Test
    void blinker_oscillatesVerticalToHorizontal() {
      //  . X .
      //  . X .
      //  . X .
      seedCells(Position.of(5, 4), Position.of(5, 5), Position.of(5, 6));

      Map<Vector4, CellType> terrain = tickAndSnapshot();

      // After one tick: horizontal row
      //  . . .
      //  X X X
      //  . . .
      assertThat(terrain.getOrDefault(pos(5, 5), CellType.EMPTY)).isEqualTo(CellType.ALIVE);
      assertThat(terrain.getOrDefault(pos(4, 5), CellType.EMPTY)).isEqualTo(CellType.ALIVE);
      assertThat(terrain.getOrDefault(pos(6, 5), CellType.EMPTY)).isEqualTo(CellType.ALIVE);
      assertThat(terrain.getOrDefault(pos(5, 4), CellType.EMPTY)).isEqualTo(CellType.EMPTY);
      assertThat(terrain.getOrDefault(pos(5, 6), CellType.EMPTY)).isEqualTo(CellType.EMPTY);
    }

    /**
     * Block (2×2 square) is a still life — it must not change after a tick.
     */
    @Test
    void block_isStillLife() {
      seedCells(
          Position.of(4, 4), Position.of(5, 4),
          Position.of(4, 5), Position.of(5, 5));

      Map<Vector4, CellType> terrain = tickAndSnapshot();

      assertThat(terrain.getOrDefault(pos(4, 4), CellType.EMPTY)).isEqualTo(CellType.ALIVE);
      assertThat(terrain.getOrDefault(pos(5, 4), CellType.EMPTY)).isEqualTo(CellType.ALIVE);
      assertThat(terrain.getOrDefault(pos(4, 5), CellType.EMPTY)).isEqualTo(CellType.ALIVE);
      assertThat(terrain.getOrDefault(pos(5, 5), CellType.EMPTY)).isEqualTo(CellType.ALIVE);
    }
  }

  // ── Orchestrate: SEED ─────────────────────────────────────────────────────

  @Nested
  class OrchestrateSeed {

    @Test
    void seed_makesListedCellsAlive() {
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.SEED,
          List.of(Position.of(1, 1), Position.of(2, 2), Position.of(3, 3)), 0f, "");

      WorldResult result = engine.orchestrate(req, state);

      assertThat(result.success()).isTrue();
      Map<Vector4, CellType> terrain = currentSnapshot();
      assertThat(terrain.get(pos(1, 1))).isEqualTo(CellType.ALIVE);
      assertThat(terrain.get(pos(2, 2))).isEqualTo(CellType.ALIVE);
      assertThat(terrain.get(pos(3, 3))).isEqualTo(CellType.ALIVE);
    }

    @Test
    void seed_isAdditive_doesNotKillExistingCells() {
      seedCells(Position.of(0, 0));
      seedCells(Position.of(9, 9));

      Map<Vector4, CellType> terrain = currentSnapshot();
      assertThat(terrain.get(pos(0, 0))).isEqualTo(CellType.ALIVE);
      assertThat(terrain.get(pos(9, 9))).isEqualTo(CellType.ALIVE);
    }

    @Test
    void seed_outOfBoundsPosition_isSkipped() {
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.SEED,
          List.of(Position.of(999, 999)), 0f, "");

      WorldResult result = engine.orchestrate(req, state);

      assertThat(result.success()).isTrue();
      // OOB cell was skipped — world remains empty
      assertThat(currentSnapshot()).isEmpty();
    }

    @Test
    void seed_emptyCellList_returnsFailed() {
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.SEED, List.of(), 0f, "");

      WorldResult result = engine.orchestrate(req, state);

      assertThat(result.success()).isFalse();
    }
  }

  // ── Orchestrate: RANDOM_SEED ──────────────────────────────────────────────

  @Nested
  class OrchestrateRandomSeed {

    @Test
    void randomSeed_density1_makesAllCellsAlive() {
      WorldResult result = engine.orchestrate(OrchestrateRequest.randomSeed(1.0f), state);

      assertThat(result.success()).isTrue();
      Map<Vector4, CellType> terrain = currentSnapshot();
      // 10×10 grid — all cells must be alive
      assertThat(terrain).hasSize(100);
      assertThat(terrain.values()).allMatch(t -> t == CellType.ALIVE);
    }

    @Test
    void randomSeed_density0_makesNoCellsAlive() {
      // Pre-seed a cell, then re-seed at density 0 — should clear everything
      seedCells(Position.of(5, 5));
      WorldResult result = engine.orchestrate(OrchestrateRequest.randomSeed(0.0f), state);

      assertThat(result.success()).isTrue();
      assertThat(currentSnapshot()).isEmpty();
    }

    @Test
    void randomSeed_invalidDensity_returnsFailed() {
      WorldResult result = engine.orchestrate(OrchestrateRequest.randomSeed(1.5f), state);

      assertThat(result.success()).isFalse();
    }
  }

  // ── Orchestrate: TOGGLE ───────────────────────────────────────────────────

  @Nested
  class OrchestrateToggle {

    @Test
    void toggle_deadCell_becomesAlive() {
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.TOGGLE,
          List.of(Position.of(3, 3)), 0f, "");
      engine.orchestrate(req, state);

      assertThat(currentSnapshot().get(pos(3, 3))).isEqualTo(CellType.ALIVE);
    }

    @Test
    void toggle_aliveCell_becomesDead() {
      seedCells(Position.of(3, 3));
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.TOGGLE,
          List.of(Position.of(3, 3)), 0f, "");
      engine.orchestrate(req, state);

      assertThat(currentSnapshot().getOrDefault(pos(3, 3), CellType.EMPTY))
          .isEqualTo(CellType.EMPTY);
    }
  }

  // ── Orchestrate: CLEAR ────────────────────────────────────────────────────

  @Nested
  class OrchestrateClear {

    @Test
    void clear_killsAllLiveCells() {
      seedCells(Position.of(1, 1), Position.of(2, 2), Position.of(9, 9));

      WorldResult result = engine.orchestrate(OrchestrateRequest.clear(), state);

      assertThat(result.success()).isTrue();
      assertThat(currentSnapshot()).isEmpty();
    }
  }

  // ── Reset ─────────────────────────────────────────────────────────────────

  @Test
  void reset_clearsAllLiveCells() {
    seedCells(Position.of(1, 1), Position.of(9, 9));

    engine.reset(state);

    assertThat(currentSnapshot()).isEmpty();
  }
}
