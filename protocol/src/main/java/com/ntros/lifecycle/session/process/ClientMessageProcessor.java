package com.ntros.lifecycle.session.process;

import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.lifecycle.session.Session;

public interface ClientMessageProcessor {

  ServerResponse process(String rawMessage, Session session);

}
