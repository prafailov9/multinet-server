package com.ntros.event.broadcaster;

import com.ntros.event.listener.SessionManager;

public final class BroadcastToAll implements Broadcaster {


  @Override
  public void publish(String serializedState, SessionManager sessions) {
    sessions.broadcast("STATE " + serializedState);
  }
}
