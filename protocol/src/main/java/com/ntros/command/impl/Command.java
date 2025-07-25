package com.ntros.command.impl;

import com.ntros.message.ProtocolContext;
import com.ntros.model.world.Message;
import java.util.Optional;

public interface Command {

  Optional<String> execute(Message message, ProtocolContext protocolContext);

}
