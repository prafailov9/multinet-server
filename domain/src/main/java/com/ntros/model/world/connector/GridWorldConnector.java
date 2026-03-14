package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.connector.ops.MoveOp;
import com.ntros.model.world.connector.ops.OrchestrateOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.engine.core.GridEngine;
import com.ntros.model.world.engine.core.PlayerGridEngine;
import com.ntros.model.world.engine.core.SimulationGridEngine;
import com.ntros.model.world.state.core.PlayerGridState;
import com.ntros.model.world.state.core.SimulationGridState;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.GridState;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GridWorldConnector implements WorldConnector {

  private final GridState state;
  private final GridEngine engine;
  private final WorldCapabilities caps;

  public GridWorldConnector(GridState state, GridEngine engine, WorldCapabilities caps) {
    this.state = state;
    this.engine = engine;
    this.caps = caps;
  }

  @Override
  public WorldResult apply(WorldOp op) {
    return switch (op) {
      case JoinOp j -> {
        if (engine instanceof PlayerGridEngine pge && state instanceof PlayerGridState pState) {
          yield pge.joinEntity(j.req(), pState);
        }
        yield WorldResult.failed(j.req().playerName(), state.worldName(),
            "World does not support player join.");
      }
      case MoveOp m -> {
        if (engine instanceof PlayerGridEngine pge && state instanceof PlayerGridState pState) {
          yield pge.storeMoveIntent(m.req(), pState);
        }
        yield WorldResult.failed(m.req().playerId(), state.worldName(),
            "World does not support player movement.");
      }
      case RemoveOp r -> {
        if (engine instanceof PlayerGridEngine pge && state instanceof PlayerGridState pState) {
          pge.removeEntity(r.removeRequest().entityId(), pState);
        }
        yield WorldResult.succeeded(r.removeRequest().entityId(), state.worldName(), "ok");
      }
      case OrchestrateOp o -> {
        if (engine instanceof SimulationGridEngine sge
            && state instanceof SimulationGridState sState) {
          yield sge.orchestrate(o.req(), sState);
        }
        yield WorldResult.failed("orchestrator", state.worldName(),
            "World does not support orchestration.");
      }
      default -> throw new IllegalStateException("Unexpected op type: " + op);
    };
  }


  @Override
  public void update() {
    engine.applyIntents(state);
  }

  /**
   * Preferred: build a typed POJO view
   */
  @Override
  public Object snapshot() {
    return engine.snapshot(state);
  }

  @Override
  public String snapshot(boolean oneLine) {
    return oneLine ? engine.serializeOneLine(state) : engine.serialize(state);
  }

  @Override
  public String getWorldName() {
    return state.worldName();
  }

  @Override
  public String getWorldType() {
    return state.worldType();
  }

  @Override
  public List<Entity> getCurrentEntities() {
    if (state instanceof PlayerGridState pState) {
      return pState.entities().values().stream().toList();
    }
    return List.of();
  }

  @Override
  public WorldCapabilities getCapabilities() {
    return caps;
  }

  @Override
  public void reset() {
    engine.reset(state);
  }

  // ── Persistence helpers ───────────────────────────────────────────────────

  /**
   * Returns the underlying {@link GridState} for read-only access by the persistence layer
   * (e.g. to save the current terrain snapshot on shutdown).
   */
  public GridState getState() {
    return state;
  }

  /**
   * Replaces the world's terrain with a previously persisted snapshot.
   *
   * <p>Intended to be called once, during server startup, before any player joins.
   * Clears the randomly-generated terrain that was produced by the constructor and loads the
   * stable, saved version so players always see the same map layout.
   *
   * @param savedTerrain terrain map loaded from the snapshot repository; must not be null
   */
//  public void restoreTerrain(Map<Vector4, TileType> savedTerrain) {
//    state.terrain().clear();
//    state.terrain().putAll(savedTerrain);
//    log.info("[GridWorldConnector] Restored terrain for '{}' ({} tiles).",
//        state.worldName(), savedTerrain.size());
//  }
}

