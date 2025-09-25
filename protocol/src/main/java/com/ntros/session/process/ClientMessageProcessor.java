package com.ntros.session.process;

import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;

public interface ClientMessageProcessor {

  ServerResponse process(String rawMessage, Session session);

}
