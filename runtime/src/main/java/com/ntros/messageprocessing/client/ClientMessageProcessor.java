package com.ntros.messageprocessing.client;

import com.ntros.lifecycle.session.Session;
import com.ntros.protocol.Message;

public interface ClientMessageProcessor {

  Message process(String rawMessage, Session session);

}
