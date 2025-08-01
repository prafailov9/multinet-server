package com.ntros.session.process;

import com.ntros.message.ProtocolContext;

public interface ClientMessageProcessor {

  String process(String rawMessage, ProtocolContext protocolContext);

}
