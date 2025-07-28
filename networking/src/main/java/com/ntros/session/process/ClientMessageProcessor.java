package com.ntros.session.process;

import com.ntros.message.ProtocolContext;
import java.util.Optional;

public interface ClientMessageProcessor {

  String process(String rawMessage, ProtocolContext protocolContext);

}
