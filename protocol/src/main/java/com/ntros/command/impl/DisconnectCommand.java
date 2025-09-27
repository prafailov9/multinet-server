package com.ntros.command.impl;

import static com.ntros.model.world.protocol.CommandType.ACK;

import com.ntros.instance.InstanceRegistry;
import com.ntros.instance.ins.Instance;
import com.ntros.message.SessionContext;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.session.Session;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * DISCONNECT client-name\n
 */
@Slf4j
public final class DisconnectCommand extends AbstractCommand {

  @Override
  public Optional<ServerResponse> execute(Message msg, Session session) {
    SessionContext ctx = session.getSessionContext();
    String worldName = ctx.getWorldName();

    // Always ACK first
    ServerResponse ack = new ServerResponse(new Message(ACK, List.of("DISCONNECT")),
        CommandResult.succeeded(ctx.getUserId(), worldName, "user disconnecting"));

    if (worldName == null) {
      // Not in a world: just ACK
      clearContext(ctx);
      return Optional.of(ack);
    }
    Instance inst = InstanceRegistry.getInstance(worldName);
    if (inst != null) {
      // Remove entity on the actor thread (async)
      String entityId = ctx.getEntityId();
      if (entityId != null && !entityId.isBlank()) {
        inst.leaveAsync(session).exceptionally(ex -> {
          log.warn("leaveAsync failed: {}", ex.toString());
          return null;
        });
        ;
      }
      // Stop receiving STATE for this client
      inst.removeSession(session);
    }

    clearContext(ctx);
    return Optional.of(ack);
  }

  /**
   * disconnecting from world, not from server, client stays authenticated
   */
  private void clearContext(SessionContext ctx) {
    ctx.setWorldId(null);
    ctx.setEntityId(null);
    ctx.setRole(null);
  }

}