package com.ntros.session.process;

import com.ntros.instance.InstanceRegistry;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseServerMessageProcessor implements ServerMessageProcessor {

  @Override
  public void processResponse(ServerResponse response, Session session) {
    Message msg = response.serverMessage();

    switch (msg.commandType()) {
      case WELCOME -> {
        // 1) Send WELCOME to the socket first (guarantee ordering).
        session.response(msg.toString());

        // 2) Then attach the session to the instance so it starts receiving STATE.
        var ctx = session.getSessionContext();
        var instance = InstanceRegistry.getInstance(ctx.getWorldName());
        if (instance != null) {
          instance.onWelcomeSent(session);  // <-- call here
        }
      }

      case ACK, ERROR -> session.response(msg.toString());
      default -> log.debug("Server message: {}", msg);
    }
  }

}
