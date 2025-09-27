package com.ntros.command.impl;

import static com.ntros.model.world.protocol.CommandType.ACK;

import com.ntros.instance.InstanceRegistry;
import com.ntros.instance.ins.Instance;
import com.ntros.message.SessionContext;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;
import java.util.List;
import java.util.Optional;

// DISCONNECT client-name\n

public final class DisconnectCommand extends AbstractCommand {

  @Override
  public Optional<ServerResponse> execute(Message msg, Session session) {
    SessionContext ctx = session.getSessionContext();
    String worldId = ctx.getWorldName();
    if (worldId == null) {
      return Optional.of(new ServerResponse(new Message(ACK, List.of("DISCONNECT")),
          CommandResult.succeeded(ctx.getUserId(), null, "user disconnected")));
    }

    Instance inst = InstanceRegistry.getInstance(worldId);
    if (inst != null) {
      WorldConnector world = WorldConnectorHolder.getWorld(worldId);
      if (world != null && ctx.getEntityId() != null) {
        world.removePlayer(ctx.getEntityId());
      }
      inst.removeSession(session);
    }

    // Clear world-related context (keep session alive if you want)
    ctx.setWorldId(null);
    ctx.setEntityId(null);
    ctx.setRole(null);

    return Optional.of(new ServerResponse(new Message(ACK, List.of("DISCONNECT")),
        CommandResult.succeeded(ctx.getUserId(), worldId, "user disconnected")));
  }
}