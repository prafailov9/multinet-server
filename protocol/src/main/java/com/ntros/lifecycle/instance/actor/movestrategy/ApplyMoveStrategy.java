package com.ntros.lifecycle.instance.actor.movestrategy;

import com.ntros.model.entity.Direction;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ApplyMoveStrategy implements MoveStrategy {


  @Override
  public CompletableFuture<CommandResult> move(WorldConnector world, MoveRequest request) {
    return null;
  }

  @Override
  public Map<String, Direction> getStagedMoves() {
    return Map.of();
  }
}
