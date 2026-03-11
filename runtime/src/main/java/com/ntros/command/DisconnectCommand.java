package com.ntros.command;

import static com.ntros.protocol.CommandType.ACK;

import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.message.SessionContext;
import com.ntros.protocol.Message;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * DISCONNECT client-name\n
 */
@Slf4j
public final class DisconnectCommand extends AbstractCommand {

  @Override
  public Message execute(Message msg, Session session) {
    SessionContext ctx = session.getSessionContext();
    String worldName = ctx.getWorldName();

    Message ack = new Message(ACK, List.of("DISCONNECT"));

    if (worldName == null) {
      clearContext(ctx);
      return ack;
    }
    Instance inst = Instances.getInstance(worldName);
    if (inst != null) {
      String entityId = ctx.getEntityId();
      if (entityId != null && !entityId.isBlank()) {
        inst.leaveAsync(session).exceptionally(ex -> {
          log.warn("leaveAsync failed: {}", ex.toString());
          return null;
        });
      }
      inst.removeSession(session);
    }

    clearContext(ctx);
    return ack;
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