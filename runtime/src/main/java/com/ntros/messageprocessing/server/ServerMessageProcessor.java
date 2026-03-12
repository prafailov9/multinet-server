package com.ntros.messageprocessing.server;

import com.ntros.lifecycle.session.Session;
import com.ntros.protocol.Message;

public interface ServerMessageProcessor {

  void processResponse(Message serverResponse, Session session);

}
