package com.ntros.event.broadcaster;

import com.ntros.event.listener.SessionManager;

public final class BroadcastToAll implements Broadcaster {

  private static final String DELIMITER = " ";
  private static final String STATE_PREFIX = "STATE";

  @Override
  public void publish(String serializedState, SessionManager sessions) {
    sessions.broadcast(STATE_PREFIX + DELIMITER + serializedState);
  }
}
