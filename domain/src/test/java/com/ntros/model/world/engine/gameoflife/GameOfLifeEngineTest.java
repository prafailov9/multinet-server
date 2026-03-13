package com.ntros.model.world.engine.gameoflife;

import static org.assertj.core.api.Assertions.assertThat;

import com.ntros.model.entity.movement.grid.Position;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.engine.d2.grid.GridWorldEngine;
import com.ntros.model.world.engine.d2.grid.gameoflife.GameOfLifeEngine;
import com.ntros.model.world.state.d2.grid.CellType;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.entity.movement.MoveInput;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.request.OrchestrateAction;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.state.d2.grid.GridWorldState;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameOfLifeEngineTest {

  private static final String WORLD = "gol-test";

  private GameOfLifeEngine engine;
  private GridWorldState state;

  @BeforeEach
  void setUp() {
    engine = new GameOfLifeEngine();
    state = GridWorldState.blank(WORLD, 10, 10);
  }

  @AfterEach
  void tearDown() {
    IdSequenceGenerator.getInstance().resetAll();
  }

  // ── Helper ──────────────────────────────────────────────────────────────

  private void setAlive(int x, int y) {
    state.terrain().put(Vector4.of(x, y, 0, 0), CellType.ALIVE);
  }

  private CellType tileAt(int x, int y) {
    return state.terrain().getOrDefault(Vector4.of(x, y, 0, 0), CellType.EMPTY);
  }

  // ── Conway survival rules ────────────────────────────────────────────────

  @Nested
  class SurvivalRules {

    @Test
    void liveCell_with2Neighbours_survives() {
      // Pattern: three cells in a row → middle survives
      setAlive(4, 5);
      setAlive(5, 5); // cell under test
      setAlive(6, 5);

      engine.applyIntents(state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.ALIVE);
    }

    @Test
    void liveCell_with3Neighbours_survives() {
      // L-shape: (5,5) has 3 neighbours
      setAlive(4, 5);
      setAlive(5, 5); // cell under test
      setAlive(6, 5);
      setAlive(5, 6);

      engine.applyIntents(state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.ALIVE);
    }

    @Test
    void liveCell_with1Neighbour_dies() {
      setAlive(4, 5);
      setAlive(5, 5); // cell under test — only 1 neighbour

      engine.applyIntents(state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.EMPTY);
    }

    @Test
    void liveCell_with4Neighbours_diesByOvercrowding() {
      setAlive(4, 4);
      setAlive(6, 4);
      setAlive(4, 6);
      setAlive(6, 6);
      setAlive(5, 5); // cell under test — 4 diagonal neighbours

      engine.applyIntents(state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.EMPTY);
    }

    @Test
    void liveCell_with0Neighbours_dies() {
      setAlive(5, 5); // isolated cell

      engine.applyIntents(state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.EMPTY);
    }
  }

  // ── Conway birth rule ─────────────────────────────────────────────────────

  @Nested
  class BirthRule {

    @Test
    void deadCell_with3Neighbours_becomesAlive() {
      // Three cells around (5,5) — cell (5,5) itself is dead
      setAlive(4, 5);
      setAlive(5, 4);
      setAlive(6, 5);

      engine.applyIntents(state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.ALIVE);
    }

    @Test
    void deadCell_with2Neighbours_staysDead() {
      setAlive(4, 5);
      setAlive(6, 5);

      engine.applyIntents(state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.EMPTY);
    }

    @Test
    void deadCell_with4Neighbours_staysDead() {
      setAlive(4, 4);
      setAlive(6, 4);
      setAlive(4, 6);
      setAlive(6, 6);

      engine.applyIntents(state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.EMPTY);
    }
  }

  // ── Stable patterns ───────────────────────────────────────────────────────

  @Nested
  class StablePatterns {

    /**
     * Blinker: three cells in a horizontal row oscillates to vertical and back.
     * After one tick the vertical becomes horizontal again after one more tick.
     */
    @Test
    void blinker_oscillatesHorizontalToVertical() {
      //  . X .
      //  . X .
      //  . X .
      setAlive(5, 4);
      setAlive(5, 5);
      setAlive(5, 6);

      engine.applyIntents(state);

      // After one tick: horizontal
      //  . . .
      //  X X X
      //  . . .
      assertThat(tileAt(5, 5)).isEqualTo(CellType.ALIVE);
      assertThat(tileAt(4, 5)).isEqualTo(CellType.ALIVE);
      assertThat(tileAt(6, 5)).isEqualTo(CellType.ALIVE);
      assertThat(tileAt(5, 4)).isEqualTo(CellType.EMPTY);
      assertThat(tileAt(5, 6)).isEqualTo(CellType.EMPTY);
    }

    /** Block (2×2 square) is a still life — it must not change. */
    @Test
    void block_isStillLife() {
      setAlive(4, 4);
      setAlive(5, 4);
      setAlive(4, 5);
      setAlive(5, 5);

      engine.applyIntents(state);

      assertThat(tileAt(4, 4)).isEqualTo(CellType.ALIVE);
      assertThat(tileAt(5, 4)).isEqualTo(CellType.ALIVE);
      assertThat(tileAt(4, 5)).isEqualTo(CellType.ALIVE);
      assertThat(tileAt(5, 5)).isEqualTo(CellType.ALIVE);
    }
  }

  // ── Static tiles pass through unchanged ──────────────────────────────────

  @Nested
  class StaticTiles {

    @Test
    void wallTile_isNeverAffectedByConwaysRules() {
      state.terrain().put(Vector4.of(5, 5, 0, 0), CellType.WALL);
      // Surround with live cells — would normally trigger overcrowding
      setAlive(4, 4); setAlive(5, 4); setAlive(6, 4);
      setAlive(4, 5);                  setAlive(6, 5);
      setAlive(4, 6); setAlive(5, 6); setAlive(6, 6);

      engine.applyIntents(state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.WALL);
    }

    @Test
    void wallTile_doesNotCountAsLiveNeighbour() {
      // (5,5) has 2 live neighbours + 1 WALL neighbour
      // Without WALL counting: only 2 live → cell stays dead
      setAlive(4, 5);
      setAlive(6, 5);
      state.terrain().put(Vector4.of(5, 4, 0, 0), CellType.WALL);

      engine.applyIntents(state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.EMPTY);
    }

    @Test
    void waterTile_passesThrough() {
      state.terrain().put(Vector4.of(3, 3, 0, 0), CellType.WATER);
      setAlive(2, 2); setAlive(3, 2); setAlive(4, 2);

      engine.applyIntents(state);

      assertThat(tileAt(3, 3)).isEqualTo(CellType.WATER);
    }
  }

  // ── Orchestrate: SEED ─────────────────────────────────────────────────────

  @Nested
  class OrchestrateSeed {

    @Test
    void seed_makesListedCellsAlive() {
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.SEED,
          List.of(Position.of(1, 1), Position.of(2, 2), Position.of(3, 3)), 0f);

      WorldResult result = engine.orchestrate(req, state);

      assertThat(result.success()).isTrue();
      assertThat(tileAt(1, 1)).isEqualTo(CellType.ALIVE);
      assertThat(tileAt(2, 2)).isEqualTo(CellType.ALIVE);
      assertThat(tileAt(3, 3)).isEqualTo(CellType.ALIVE);
    }

    @Test
    void seed_isAdditive_doesNotKillExistingCells() {
      setAlive(0, 0);
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.SEED,
          List.of(Position.of(9, 9)), 0f);

      engine.orchestrate(req, state);

      assertThat(tileAt(0, 0)).isEqualTo(CellType.ALIVE);
      assertThat(tileAt(9, 9)).isEqualTo(CellType.ALIVE);
    }

    @Test
    void seed_outOfBoundsPosition_isSkipped() {
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.SEED,
          List.of(Position.of(999, 999)), 0f);

      WorldResult result = engine.orchestrate(req, state);

      // Succeeds but the OOB cell is skipped — world remains empty
      assertThat(result.success()).isTrue();
      assertThat(state.terrain().values()).doesNotContain(CellType.ALIVE);
    }

    @Test
    void seed_emptyCellList_returnsFailed() {
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.SEED, List.of(), 0f);

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
      assertThat(state.terrain().values()).allMatch(t -> t == CellType.ALIVE);
    }

    @Test
    void randomSeed_density0_makesNoCellsAlive() {
      setAlive(5, 5); // pre-existing
      WorldResult result = engine.orchestrate(OrchestrateRequest.randomSeed(0.0f), state);

      assertThat(result.success()).isTrue();
      assertThat(state.terrain().values()).doesNotContain(CellType.ALIVE);
    }

    @Test
    void randomSeed_doesNotOverwriteStaticTiles() {
      state.terrain().put(Vector4.of(5, 5, 0, 0), CellType.WALL);

      engine.orchestrate(OrchestrateRequest.randomSeed(1.0f), state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.WALL);
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
          List.of(Position.of(3, 3)), 0f);

      engine.orchestrate(req, state);

      assertThat(tileAt(3, 3)).isEqualTo(CellType.ALIVE);
    }

    @Test
    void toggle_aliveCell_becomesDead() {
      setAlive(3, 3);
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.TOGGLE,
          List.of(Position.of(3, 3)), 0f);

      engine.orchestrate(req, state);

      assertThat(tileAt(3, 3)).isEqualTo(CellType.EMPTY);
    }

    @Test
    void toggle_staticTile_isNotFlipped() {
      state.terrain().put(Vector4.of(3, 3, 0, 0), CellType.WALL);
      OrchestrateRequest req = new OrchestrateRequest(OrchestrateAction.TOGGLE,
          List.of(Position.of(3, 3)), 0f);

      engine.orchestrate(req, state);

      assertThat(tileAt(3, 3)).isEqualTo(CellType.WALL);
    }
  }

  // ── Orchestrate: CLEAR ────────────────────────────────────────────────────

  @Nested
  class OrchestrateClear {

    @Test
    void clear_killsAllLiveCells() {
      setAlive(1, 1); setAlive(2, 2); setAlive(9, 9);

      WorldResult result = engine.orchestrate(OrchestrateRequest.clear(), state);

      assertThat(result.success()).isTrue();
      assertThat(state.terrain().values()).doesNotContain(CellType.ALIVE);
    }

    @Test
    void clear_preservesStaticTiles() {
      state.terrain().put(Vector4.of(5, 5, 0, 0), CellType.WALL);
      setAlive(1, 1);

      engine.orchestrate(OrchestrateRequest.clear(), state);

      assertThat(tileAt(5, 5)).isEqualTo(CellType.WALL);
      assertThat(tileAt(1, 1)).isEqualTo(CellType.EMPTY);
    }
  }

  // ── Non-orchestrated worlds: default method ───────────────────────────────

  @Test
  void nonOrchestratedEngine_defaultOrchestrate_returnsFailed() {
    // GridWorldEngine inherits the default WorldEngine.orchestrate() impl
    var gridEngine = new GridWorldEngine();
    WorldResult result = gridEngine.orchestrate(OrchestrateRequest.clear(), state);

    assertThat(result.success()).isFalse();
    assertThat(result.reason()).contains("not support");
  }

  // ── Player lifecycle ─────────────────────────────────────────────────────

  @Nested
  class PlayerLifecycle {

    @Test
    void joinEntity_registersSpectator_inEntitiesMap() {
      WorldResult result = engine.joinEntity(new JoinRequest("alice"), state);

      assertThat(result.success()).isTrue();
      assertThat(state.entities()).containsKey("alice");
    }

    @Test
    void joinEntity_spectator_notInTakenPositions() {
      engine.joinEntity(new JoinRequest("alice"), state);

      assertThat(state.takenPositions()).isEmpty();
    }

    @Test
    void joinEntity_duplicate_returnsFailed() {
      engine.joinEntity(new JoinRequest("alice"), state);
      WorldResult second = engine.joinEntity(new JoinRequest("alice"), state);

      assertThat(second.success()).isFalse();
    }

    @Test
    void removeEntity_removesSpectator() {
      engine.joinEntity(new JoinRequest("alice"), state);

      engine.removeEntity("alice", state);

      assertThat(state.entities()).doesNotContainKey("alice");
    }

    @Test
    void storeMoveIntent_returnsFailed() {
      MoveRequest req = new MoveRequest("alice", new MoveInput(1, 0, 0, 0));
      WorldResult result = engine.storeMoveIntent(req, state);

      assertThat(result.success()).isFalse();
    }
  }

  // ── Reset ─────────────────────────────────────────────────────────────────

  @Test
  void reset_clearsAllLiveCellsAndSpectators() {
    setAlive(1, 1); setAlive(9, 9);
    engine.joinEntity(new JoinRequest("alice"), state);

    engine.reset(state);

    assertThat(state.terrain().values()).doesNotContain(CellType.ALIVE);
    assertThat(state.entities()).isEmpty();
  }
}
