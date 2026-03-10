package com.ntros.command.impl;

import com.ntros.protocol.Message;
import com.ntros.protocol.response.ServerResponse;
import com.ntros.lifecycle.session.Session;
import java.util.Optional;

public class StateCommand implements Command {

  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    return Optional.empty();
  }
}
