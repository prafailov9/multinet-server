package com.ntros.command;

import static com.ntros.protocol.Message.authSuccess;

import com.ntros.lifecycle.session.Session;
import com.ntros.message.SessionContext;
import com.ntros.protocol.Message;

/**
 * Class to authenticate the client with the server without joining or creating a world
 */
public class AuthCommand extends AbstractCommand {

  private static final String CLIENT_AUTHENTICATED = "CLIENT_AUTHENTICATED";
  private static final String NOT_IN_WORLD = "NOT_IN_WORLD";

  @Override
  public Message execute(Message message, Session session) {
    SessionContext sessionContext = session.getSessionContext();
    if (sessionContext.isAuthenticated()) {
      return Message.errorMsg("User already authenticated");
    }

    sessionContext.setAuthenticated(true);
    // TODO: create Constants class for these markers
    if ((sessionContext.getWorldName() == null || sessionContext.getWorldName().isBlank()) && (
        sessionContext.getEntityId() == null || sessionContext.getEntityId().isBlank())) {
      sessionContext.setEntityId(CLIENT_AUTHENTICATED);
      sessionContext.setWorldId(NOT_IN_WORLD);
    }

    return authSuccess(sessionContext.getSessionId());
  }
}
