package com.ntros.model.world.connector;

import com.ntros.model.entity.Direction;
import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.connector.ops.MoveOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.model.world.state.solid.GridWorldState;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GridWorldConnector implements WorldConnector {

  private final GridWorldState state;
  private final GridWorldEngine engine;
  private final WorldCapabilities caps;

  public GridWorldConnector(GridWorldState state, GridWorldEngine engine, WorldCapabilities caps) {
    this.state = state;
    this.engine = engine;
    this.caps = caps;
  }

  @Override
  public CommandResult apply(WorldOp op) {
    return switch (op) {
      case JoinOp j -> engine.joinEntity(j.req(), state);
      case MoveOp m -> engine.storeMoveIntent(m.req(), state);
      case RemoveOp r -> {
        engine.removeEntity(r.removeRequest().entityId(), state);
        yield CommandResult.succeeded(r.removeRequest().entityId(), state.worldName(), "ok");
      }
    };
  }


  @Override
  public void update() {
    engine.applyIntents(state);
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
}

