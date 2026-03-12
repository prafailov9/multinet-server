package com.ntros.messageprocessing.server;

import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.protocol.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseServerMessageProcessor implements ServerMessageProcessor {

  private static final String DISCONNECT_RESPONSE = "DISCONNECT";

  @Override
  public void processResponse(Message serverResponse, Session session) {
    switch (serverResponse.commandType()) {
      case WELCOME -> onWelcome(serverResponse, session);
      case ACK -> onAck(serverResponse, session);
      case ORCHESTRATE_SUCCESS -> onOrchestrationSuccess(serverResponse, session);
      default -> {
        log.debug("Server message: {}", serverResponse);
        session.response(serverResponse.toString());
      }
    }
  }


  private void onWelcome(Message serverResponse, Session session) {
    log.info("Processing Welcome Response.");
    // 1. Send WELCOME to the socket first (guarantee ordering)
    session.response(serverResponse.toString());

    log.info("Processing Welcome Response.");
    // 2. Then attach the session to the instance so it starts receiving STATE
    attachToInstance(session);
  }

  private void onAck(Message serverResponse, Session session) {
    if (!serverResponse.args().isEmpty() && DISCONNECT_RESPONSE.equals(
        serverResponse.args().getFirst())) {
      // session is already removed from the instance in the command.
      // Now close the socket / stop the session loop promptly.
      // signal to stop session on next loop
      session.stop();
    }
    session.response(serverResponse.toString());
  }

  private void onOrchestrationSuccess(Message serverResponse, Session session) {
    attachToInstance(session);
  }

  private void attachToInstance(Session session) {
    var ctx = session.getSessionContext();
    var instance = Instances.getInstance(ctx.getWorldName());
    if (instance != null) {
      log.info("Registering client to instance.");
      instance.registerSession(session);
    }

  }
}
