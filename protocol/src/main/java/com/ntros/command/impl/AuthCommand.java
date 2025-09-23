package com.ntros.command.impl;

import com.ntros.message.ProtocolContext;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;
import java.util.Optional;

/**
 * Class to authenticate the client with the server without joining or creating a world
 */
public class AuthCommand extends AbstractCommand {

  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    ProtocolContext protocolContext = session.getProtocolContext();
    if (protocolContext.isAuthenticated()) {
      return Optional.of(ServerResponse.ofError(message, "User already authenticated"));
    }

    protocolContext.setAuthenticated(true);
    // TODO: create Constants class for these markers
    // marker for clients that are authenticated into the server, but are not in a world.
    protocolContext.setPlayerId("NON_PLAYER_AUTHENTICATED");
    protocolContext.setWorldId("NOT_IN_WORLD");
    return Optional.of(ServerResponse.ofSuccess(message, "User successfully authenticated"));
  }
}
