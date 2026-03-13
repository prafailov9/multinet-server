package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.connector.ops.MoveOp;
import com.ntros.model.world.connector.ops.OrchestrateOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.engine.core.WorldEngine;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.state.solid.GridWorldState;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GridWorldConnector implements WorldConnector {

  private final GridWorldState state;
  private final WorldEngine engine;
  private final WorldCapabilities caps;

  public GridWorldConnector(GridWorldState state, WorldEngine engine, WorldCapabilities caps) {
    this.state = state;
    this.engine = engine;
    this.caps = caps;
  }

  @Override
  public WorldResult apply(WorldOp op) {
    return switch (op) {
      case JoinOp j -> engine.joinEntity(j.req(), state);
      case MoveOp m -> engine.storeMoveIntent(m.req(), state);
      case RemoveOp r -> {
        engine.removeEntity(r.removeRequest().entityId(), state);
        yield WorldResult.succeeded(r.removeRequest().entityId(), state.worldName(), "ok");
      }
      case OrchestrateOp o -> engine.orchestrate(o.req(), state);
      default -> throw new IllegalStateException("Unexpected value: " + op);
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
    return state.entities().values().stream().toList();
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
   * Returns the underlying {@link GridWorldState} for read-only access by the persistence layer
   * (e.g. to save the current terrain snapshot on shutdown).
   */
  public GridWorldState getState() {
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

