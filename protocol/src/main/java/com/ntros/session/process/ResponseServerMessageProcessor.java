package com.ntros.session.process;

import com.ntros.instance.Instances;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.session.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseServerMessageProcessor implements ServerMessageProcessor {

  @Override
  public void processResponse(ServerResponse response, Session session) {
    Message msg = response.serverMessage();

    // 1) Send WELCOME to the socket first (guarantee ordering).
    session.response(msg.toString());
    switch (msg.commandType()) {
      case WELCOME -> {

        // 2) Then attach the session to the instance so it starts receiving STATE.
        var ctx = session.getSessionContext();
        var instance = Instances.getInstance(ctx.getWorldName());
        if (instance != null) {
          instance.registerSession(session);
        }
      }

      case ACK -> processAck(msg, session);
      case ERROR -> session.response(msg.toString());
      default -> log.debug("Server message: {}", msg);
    }
  }

  private void processAck(Message msg, Session session) {
    if (!msg.args().isEmpty() && "DISCONNECT".equals(msg.args().getFirst())) {
      // We already removed the session from the instance in the command.
      // Now close the socket / stop the session loop promptly.
      // signal to stop session on next loop
      session.stop();
    }

  }
}
