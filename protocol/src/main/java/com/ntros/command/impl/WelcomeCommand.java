package com.ntros.command.impl;

import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;
import java.util.Optional;

public class WelcomeCommand implements Command {

  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    return Optional.empty();
  }
}
