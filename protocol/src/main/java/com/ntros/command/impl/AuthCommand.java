package com.ntros.command.impl;

import com.ntros.message.SessionContext;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.CommandType;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;
import java.util.List;
import java.util.Optional;

/**
 * Class to authenticate the client with the server without joining or creating a world
 */
public class AuthCommand extends AbstractCommand {

  private static final String CLIENT_AUTHENTICATED = "CLIENT_AUTHENTICATED";
  private static final String NOT_IN_WORLD = "NOT_IN_WORLD";

  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    SessionContext sessionContext = session.getSessionContext();
    if (sessionContext.isAuthenticated()) {
      return Optional.of(ServerResponse.ofError(message, "User already authenticated"));
    }

    sessionContext.setAuthenticated(true);
    // TODO: create Constants class for these markers
    // marker for clients that are authenticated into the server, but are not in a world.
    if ((sessionContext.getWorldName() == null || sessionContext.getWorldName().isBlank()) && (
        sessionContext.getEntityId() == null || sessionContext.getEntityId().isBlank())) {
      sessionContext.setEntityId(CLIENT_AUTHENTICATED);
      sessionContext.setWorldId(NOT_IN_WORLD);
    }
    return Optional.of(
        ServerResponse.ofSuccess(new Message(CommandType.AUTH_SUCCESS, List.of()),
            new CommandResult(true, sessionContext.getEntityId(),
                sessionContext.getWorldName(), "User successfully authenticated")));
  }
}
