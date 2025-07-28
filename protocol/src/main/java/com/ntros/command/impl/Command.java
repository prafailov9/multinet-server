package com.ntros.command.impl;

import com.ntros.message.ProtocolContext;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import java.util.Optional;

public interface Command {

  Optional<ServerResponse> execute(Message message, ProtocolContext protocolContext);

}
