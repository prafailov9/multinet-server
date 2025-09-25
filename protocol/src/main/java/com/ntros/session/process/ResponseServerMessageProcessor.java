package com.ntros.session.process;

import static com.ntros.event.SessionEvent.sessionFailed;
import static com.ntros.event.SessionEvent.sessionStarted;

import com.ntros.event.bus.SessionEventBus;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.CommandType;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseServerMessageProcessor implements ServerMessageProcessor {


  @Override
  public void processResponse(ServerResponse response, Session session) {
    boolean isAuth = session.getProtocolContext().isAuthenticated();

    if (!isAuth) {
      log.info("Client not authenticated: {}. Sending failed session event to Bus",
          session.getProtocolContext());
      SessionEventBus.get()
          .publish(sessionFailed(session, response.commandResult().reason()));
    }

    Message msg = response.serverMessage();
    CommandResult result = response.commandResult();

    switch (msg.commandType()) {
      case WELCOME -> {
        log.info("Sending start session event. Sharing world state with user: {}",
            session.getProtocolContext().getSessionId());
        SessionEventBus.get().publish(
            sessionStarted(session, "Starting client session...", msg.toString())
        );
      }
      case AUTH_SUCCESS -> {
        // successful authentication. Client can access/create worlds.
      }
      case ACK -> // Server already ticking the state; just log
          log.info("ACK from server (player={}, world={}): {}",
              result.playerName(), result.worldName(), result.reason());

      case ERROR -> {
        log.warn("Server error (player={}, world={}): {}",
            result.playerName(), result.worldName(), result.reason());
        SessionEventBus.get().publish(sessionFailed(session, result.reason()));
      }

      default -> log.warn("Unhandled command type {} with args: {}", msg.commandType(), msg.args());
    }
  }

}
