package com.ntros.lifecycle.session.process;

import com.ntros.protocol.response.ServerResponse;
import com.ntros.lifecycle.session.Session;

public interface ClientMessageProcessor {

  ServerResponse process(String rawMessage, Session session);

}
