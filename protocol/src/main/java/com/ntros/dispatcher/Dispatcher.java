package com.ntros.dispatcher;

import com.ntros.message.ProtocolContext;
import com.ntros.model.world.Message;
import java.util.Optional;

public interface Dispatcher {

  Optional<String> dispatch(Message message, ProtocolContext protocolContext);

}
