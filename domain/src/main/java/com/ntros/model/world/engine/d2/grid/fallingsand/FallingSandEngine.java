package com.ntros.model.world.engine.d2.grid.fallingsand;

import com.ntros.model.entity.Entity;
import com.ntros.model.world.engine.d2.grid.AbstractGridEngine;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.GridState;

public class FallingSandEngine extends AbstractGridEngine {

  @Override
  public void applyIntents(GridState state) {

  }

  @Override
  public WorldResult storeMoveIntent(MoveRequest move, GridState state) {
    return null;
  }

  @Override
  public WorldResult joinEntity(JoinRequest joinRequest, GridState state) {
    return null;
  }

  @Override
  public Entity removeEntity(String entityId, GridState state) {
    return null;
  }

  @Override
  public String serialize(GridState state) {
    return "";
  }

  @Override
  public String serializeOneLine(GridState state) {
    return "";
  }

  @Override
  public void reset(GridState state) {

  }
}
