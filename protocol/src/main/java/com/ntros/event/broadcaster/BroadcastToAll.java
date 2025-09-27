package com.ntros.event.broadcaster;

import com.ntros.event.sessionmanager.SessionManager;

public final class BroadcastToAll implements Broadcaster {

  @Override
  public void publish(String serializedState, SessionManager sessions) {
    sessions.broadcast(serializedState);
  }
}
