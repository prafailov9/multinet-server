package com.ntros.command.impl;

import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.session.Session;
import java.util.Optional;

public class OrchestratorCommand extends AbstractCommand {

  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    return Optional.empty();
  }
}
