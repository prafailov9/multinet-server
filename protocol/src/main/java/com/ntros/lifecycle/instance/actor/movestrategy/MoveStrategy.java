package com.ntros.lifecycle.instance.actor.movestrategy;

import com.ntros.model.entity.Direction;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface MoveStrategy {

  CompletableFuture<CommandResult> move(WorldConnector world, MoveRequest request);

  Map<String, Direction> getStagedMoves();

}
