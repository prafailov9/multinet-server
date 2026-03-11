package com.ntros.command;

import static com.ntros.protocol.CommandType.ACK;
import static com.ntros.protocol.Message.errorMsg;

import com.ntros.command.exception.DisconnectCmdException;
import com.ntros.lifecycle.LifecycleHooks;
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
    try {
      SessionContext ctx = session.getSessionContext();
      checkNotAuth(ctx);
      String worldName = ctx.getWorldName();

      // if client not in a world, just clear and return
      if (worldName == null) {
        clearContext(ctx);
        return new Message(ACK, List.of("DISCONNECT"));
      }
      Instance inst = getInstance(worldName);
      String entityId = ctx.getEntityId();
      if (entityId != null && !entityId.isBlank()) {
        // Fire persistence / analytics hook BEFORE clearing context so hooks can read player name
        LifecycleHooks.firePlayerLeave(entityId, worldName);
        inst.leaveAsync(session).exceptionally(ex -> {
          log.warn("leaveAsync failed: {}", ex.toString());
          return null;
        });
      }
      inst.removeSession(session);

      clearContext(ctx);
      return new Message(ACK, List.of("DISCONNECT"));
    } catch (DisconnectCmdException ex) {
      log.error("Error during Disconnect: {}", ex.getMessage(), ex);
      return errorMsg(ex.getMessage());
    }
  }

  /**
   * disconnecting from world, not from server, client stays authenticated
   */
  private void clearContext(SessionContext ctx) {
    ctx.setWorldId(null);
    ctx.setEntityId(null);
    ctx.setRole(null);
  }

  private void checkNotAuth(SessionContext sessionContext) {
    if (!sessionContext.isAuthenticated()) {
      throw new DisconnectCmdException("Client already disconnected");
    }
  }

  private Instance getInstance(String world) {
    Instance inst = Instances.getInstance(world);
    if (inst == null) {
      throw new DisconnectCmdException(String.format("Instance not exist for world: %s", world));
    }
    return inst;
  }

}