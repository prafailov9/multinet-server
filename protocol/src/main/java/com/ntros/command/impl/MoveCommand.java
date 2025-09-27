package com.ntros.command.impl;

import static com.ntros.model.world.protocol.CommandType.ACK;

import com.ntros.instance.InstanceRegistry;
import com.ntros.instance.ins.Instance;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.Direction;
import com.ntros.model.world.protocol.CommandType;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveCommand extends AbstractCommand {

  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    validateContext(ctx);

    String dir = message.args().getFirst();
    String player = ctx.getEntityId();
    Instance instance = InstanceRegistry.getInstance(ctx.getWorldName());

    CommandResult r = instance.move(new MoveRequest(player, Direction.valueOf(dir)));
    return Optional.of(new ServerResponse(new Message(ACK, List.of(dir)), r));
  }

  private ServerResponse handleResult(CommandResult commandResult, String move) {
    log.info("Received commandResult from world: {}", commandResult);
    return commandResult.success()
        ? new ServerResponse(new Message(ACK, List.of(move)), commandResult)
        : new ServerResponse(new Message(CommandType.ERROR, List.of(commandResult.reason())),
            commandResult);
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
