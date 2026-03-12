package com.ntros.messageprocessing.server;

import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.protocol.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseServerMessageProcessor implements ServerMessageProcessor {

  @Override
  public void processResponse(Message serverResponse, Session session) {
    switch (serverResponse.commandType()) {
      case WELCOME -> onWelcome(serverResponse, session);
      case ACK -> onAck(serverResponse, session);
      default -> {
        log.debug("Server message: {}", serverResponse);
        session.response(serverResponse.toString());
      }
    }
  }


  private void onWelcome(Message serverResponse, Session session) {
    // 1. Send WELCOME to the socket first (guarantee ordering)
    session.response(serverResponse.toString());

    // 2. Then attach the session to the instance so it starts receiving STATE
    var ctx = session.getSessionContext();
    var instance = Instances.getInstance(ctx.getWorldName());
    if (instance != null) {
      instance.registerSession(session);
    }
  }

  private void onAck(Message serverResponse, Session session) {
    if (!serverResponse.args().isEmpty() && "DISCONNECT".equals(serverResponse.args().getFirst())) {
      // session is already removed from the instance in the command.
      // Now close the socket / stop the session loop promptly.
      // signal to stop session on next loop
      session.stop();
    }
    session.response(serverResponse.toString());
  }
}
