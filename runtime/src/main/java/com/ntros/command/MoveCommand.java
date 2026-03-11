package com.ntros.command;

import static com.ntros.protocol.CommandType.ACK;
import static com.ntros.protocol.CommandType.ERROR;

import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.Direction;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.protocol.Message;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveCommand extends AbstractCommand {

  @Override
  public Message execute(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    validateContext(ctx);

    String rawDir = message.args().getFirst();
    if (rawDir == null || rawDir.isBlank()) {
      return new Message(ERROR, List.of("MISSING_DIRECTION"));
    }

    // move command curently only handles movement(n,w,s,e) within a grid(x, y)
    // TODO: extend to handle 3d + 4d movement. Need to extend the Movement semantics in domain
    Direction dir;
    try {
      dir = Direction.valueOf(rawDir.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return new Message(ERROR, List.of("INVALID_DIRECTION"));
    }

    String playerId = ctx.getEntityId();
    Instance instance = Instances.getInstance(ctx.getWorldName());
    if (instance == null) {
      return new Message(ERROR, List.of("WORLD_NOT_FOUND"));
    }

    instance.storeMoveAsync(new MoveRequest(playerId, dir))
        .exceptionally(ex -> {
          log.warn("MOVE failed later for player={} world={} : {}",
              playerId, ctx.getWorldName(), ex.toString());
          return null;
        });

    return new Message(ACK, List.of(dir.name()));

  }
}
