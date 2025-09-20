package com.ntros.event.listener;

import com.ntros.event.SessionEvent;
import com.ntros.instance.Instance;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.session.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstanceSessionEventListener implements SessionEventListener {

  private final Instance instance;

  public InstanceSessionEventListener(Instance instance) {
    this.instance = instance;
  }


  @Override
  public void onSessionEvent(SessionEvent sessionEvent) {
    switch (sessionEvent.getEventType()) {
      case SESSION_STARTED -> started(sessionEvent.getSession(), sessionEvent.getServerMessage());
      case SESSION_CLOSED -> closed(sessionEvent.getSession());
      case SESSION_FAILED -> failed(sessionEvent.getSession());
    }
  }

  /**
   * Registers the current session. Starts the Tick Server on first registered session.
   */
  private void started(Session session, String serverWelcomeMessage) {
    // indicates a successful JOIN command
    instance.registerSession(session);
    // send welcome response to client to trigger UI changes
    session.response(serverWelcomeMessage);
    if (instance.getActiveSessionsCount() > 0 && !instance.isRunning()) {
      instance.run();
    }
  }

  private void closed(Session session) {
    instance.removeSession(session);
    removeSessionEntityFromWorld(session.getProtocolContext());
    closeAll();
  }

  private void failed(Session session) {
    instance.removeSession(session);
    removeSessionEntityFromWorld(session.getProtocolContext());
    closeAll();
  }

  /**
   * stops session and tick scheduler
   */
  private void closeAll() {
    // Check if there are any remaining active sessions.
    if (instance.getActiveSessionsCount() == 0 && instance.isRunning()) {
      instance.reset();
      log.info("Last session was closed. Ticker stopped.");
    }
  }

  private void removeSessionEntityFromWorld(ProtocolContext context) {
    if (context == null || !context.isAuthenticated()) {
      log.warn("IN EVENT_LISTENER- removeEntity from World: sessionContext is invalid: {}. ",
          context);
      return;
    }
    if (context.getSessionId() != null && context.getWorldId() != null && !context.getWorldId()
        .isEmpty()) {
      WorldConnector worldConnector = WorldConnectorHolder.getWorld(context.getWorldId());
      log.info("IN EVENT_LISTENER: Removing entity {} from world {}. ", context,
          worldConnector.worldName());
      worldConnector.remove(context.getPlayerId());
    }
  }
}
