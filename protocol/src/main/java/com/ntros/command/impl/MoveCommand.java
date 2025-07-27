package com.ntros.command.impl;

import com.ntros.message.ProtocolContext;
import com.ntros.model.entity.Direction;
import com.ntros.model.world.Message;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.ServerResponse;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveCommand extends AbstractCommand {

  @Override
  public Optional<String> execute(Message message, ProtocolContext protocolContext) {
    validateContext(protocolContext);

    WorldConnector world = WorldConnectorHolder.getWorld(protocolContext.getWorldId());
    Direction direction = resolveMoveIntent(message);
    ServerResponse serverResponse = world.storeMoveIntent(
        new MoveRequest(protocolContext.getPlayerId(), direction));

    return handleResult(serverResponse, direction.name());
  }

  private Optional<String> handleResult(ServerResponse serverResponse, String move) {
    log.info("Received serverResponse from world: {}", serverResponse);
    return serverResponse.success() ? Optional.of(String.format("ACK %s\n", move))
        : Optional.of(String.format("ERROR %s\n", serverResponse.reason()));
  }

  private Direction resolveMoveIntent(Message message) {
    // move has to be second argument of command.
    String move = message.args().getFirst();
    if (move == null || move.isEmpty()) {
      logAndThrow("Move cannot be empty.");
    }
    return Direction.valueOf(move); // throws illegalArgument
  }
}
