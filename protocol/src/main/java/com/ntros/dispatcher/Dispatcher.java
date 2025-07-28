package com.ntros.dispatcher;

import com.ntros.message.ProtocolContext;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import java.util.Optional;

public interface Dispatcher {

  Optional<ServerResponse> dispatch(Message message, ProtocolContext protocolContext);

}
