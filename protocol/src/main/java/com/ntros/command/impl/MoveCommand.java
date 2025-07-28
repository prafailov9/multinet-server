package com.ntros.command.impl;

import com.ntros.message.ProtocolContext;
import com.ntros.model.entity.Direction;
import com.ntros.model.world.protocol.CommandType;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.ServerResponse;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveCommand extends AbstractCommand {

  @Override
  public Optional<ServerResponse> execute(Message message, ProtocolContext protocolContext) {
    validateContext(protocolContext);

    WorldConnector world = WorldConnectorHolder.getWorld(protocolContext.getWorldId());
    Direction direction = resolveMoveIntent(message);
    CommandResult commandResult = world.storeMoveIntent(
        new MoveRequest(protocolContext.getPlayerId(), direction));

    return Optional.of(handleResult(commandResult, direction.name()));
  }

  private ServerResponse handleResult(CommandResult commandResult, String move) {
    log.info("Received commandResult from world: {}", commandResult);
    return commandResult.success()
        ? new ServerResponse(new Message(CommandType.ACK, List.of(move)), commandResult)
        : new ServerResponse(new Message(CommandType.ERROR, List.of(commandResult.reason())), commandResult);
//    return commandResult.success() ? Optional.of(String.format("ACK %s\n", move))
//        : Optional.of(String.format("ERROR %s\n", commandResult.reason()));
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
