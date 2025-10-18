package com.ntros.lifecycle.instance.actor.movestrategy;

import com.ntros.model.entity.Direction;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StageMovesStrategy implements MoveStrategy {

  /**
   * last-write-wins move coalescing
   */
  private final ConcurrentHashMap<String, Direction> stagedMoves = new ConcurrentHashMap<>();


  @Override
  public CompletableFuture<CommandResult> move(WorldConnector world, MoveRequest request) {
    stagedMoves.put(request.playerId(), request.direction()); // last-write-wins
    return CompletableFuture.completedFuture(
        CommandResult.succeeded(request.playerId(), world.getWorldName(), "queued"));
  }

  @Override
  public Map<String, Direction> getStagedMoves() {
    return stagedMoves;
  }
}
