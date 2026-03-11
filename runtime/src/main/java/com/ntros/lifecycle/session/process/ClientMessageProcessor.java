package com.ntros.lifecycle.session.process;

import com.ntros.lifecycle.session.Session;
import com.ntros.protocol.Message;

public interface ClientMessageProcessor {

  Message process(String rawMessage, Session session);

}
