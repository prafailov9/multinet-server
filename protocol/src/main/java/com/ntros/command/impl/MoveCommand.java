package com.ntros.command.impl;

import static com.ntros.model.world.protocol.CommandType.ACK;
import static com.ntros.model.world.protocol.CommandType.ERROR;

import com.ntros.instance.Instances;
import com.ntros.instance.ins.Instance;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.Direction;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.session.Session;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoveCommand extends AbstractCommand {

  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    validateContext(ctx); // ensure joined/auth’d, has world/entity

    // Parse & validate direction defensively
    String rawDir = message.args().getFirst();
    if (rawDir == null || rawDir.isBlank()) {
      return Optional.of(new ServerResponse(
          new Message(ERROR, List.of("MISSING_DIRECTION")),
          CommandResult.failed(ctx.getEntityId(), ctx.getWorldName(), "missing direction")));
    }

    Direction dir;
    try {
      dir = Direction.valueOf(rawDir.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return Optional.of(new ServerResponse(
          new Message(ERROR, List.of("INVALID_DIRECTION")),
          CommandResult.failed(ctx.getEntityId(), ctx.getWorldName(), "invalid direction")));
    }

    String playerId = ctx.getEntityId();
    Instance instance = Instances.getInstance(ctx.getWorldName());
    if (instance == null) {
      return Optional.of(new ServerResponse(
          new Message(ERROR, List.of("WORLD_NOT_FOUND")),
          CommandResult.failed(playerId, null, "world not found")));
    }

    // Enqueue asynchronously; do NOT block.
    instance.storeMoveAsync(new MoveRequest(playerId, dir))
        .exceptionally(ex -> {
          log.warn("MOVE failed later for player={} world={} : {}",
              playerId, ctx.getWorldName(), ex.toString());
          return null;
        });

    // Immediate ACK so client stays snappy; STATE will arrive on next tick.
    return Optional.of(new ServerResponse(
        new Message(ACK, List.of(dir.name())),
        CommandResult.succeeded(playerId, ctx.getWorldName(), "queued")));
  }
}
