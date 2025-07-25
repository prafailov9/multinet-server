package com.ntros.command.impl;

import com.ntros.message.ProtocolContext;
import com.ntros.model.world.Message;
import java.util.Optional;

public class DisconnectCommand implements Command {

  @Override
  public Optional<String> execute(Message message, ProtocolContext protocolContext) {
    // DISCONNECT client-name\n

    return Optional.empty();
  }
}
