package com.ntros.model.world.connector;

import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.state.solid.GridWorldState;

@FunctionalInterface
public interface WorldOp {
  CommandResult apply(GridWorldEngine engine, GridWorldState state);
}