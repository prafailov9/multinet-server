package com.ntros.command;

import static com.ntros.protocol.CommandType.ACK;
import static com.ntros.protocol.CommandType.ERROR;
import static com.ntros.protocol.Message.errorMsg;

import com.ntros.command.exception.MoveCmdException;
import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.Direction;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.protocol.CommandType;
import com.ntros.protocol.Message;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveCommand extends AbstractCommand {

  @Override
  public Message execute(Message message, Session session) {
    try {
      SessionContext ctx = session.getSessionContext();
      validateContext(ctx);
      validateMessage(message);

      // move command curently only handles movement(n,w,s,e) within a grid(x, y)
      // TODO: extend to handle 3d + 4d movement. Need to extend the Movement semantics in domain
      Direction dir = Direction.valueOf(message.args().getFirst().toUpperCase(Locale.ROOT));

      String playerId = ctx.getEntityId();
      Instance instance = getInstance(ctx.getWorldName());

      instance.storeMoveAsync(new MoveRequest(playerId, dir))
          .exceptionally(ex -> {
            log.warn("MOVE failed later for player={} world={} : {}", playerId, ctx.getWorldName(),
                ex.toString());
            return null;
          });

      return new Message(ACK, List.of(dir.name()));

    } catch (MoveCmdException | IllegalArgumentException ex) {
      log.error("Error during MoveCommand processing: {}", ex.getMessage(), ex);
      return errorMsg(ex.getMessage());
    }
  }

  private Instance getInstance(String world) {
    Instance instance = Instances.getInstance(world);
    if (instance == null) {
      throw new MoveCmdException(String.format("Instance not found for worldName: %s", world));
    }
    return instance;
  }

  private void validateMessage(Message message) {
    if (!message.commandType().equals(CommandType.MOVE)) {
      throw new MoveCmdException(
          String.format("Wrong command type in MoveCommand: %s", message.commandType()));
    }
    if (message.args() == null || message.args().isEmpty()) {
      throw new MoveCmdException(String.format("Empty argument list. Full Message: %s", message));
    }

    if (message.args().getFirst().isBlank()) {
      throw new MoveCmdException(
          String.format("Argument list missing direction. Full Message: %s", message));
    }
  }

}
