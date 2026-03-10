package com.ntros.event.broadcaster;

import com.ntros.event.sessionmanager.SessionManager;

public final class SingleBroadcaster implements Broadcaster {

  private final String ownerUserId;

  public SingleBroadcaster(String ownerUserId) {
    this.ownerUserId = ownerUserId;
  }


  @Override
  public void publish(String serializedState, SessionManager sessions) {
    String msg = "STATE " + serializedState;
    sessions.getActiveSessions().stream()
        .filter(s -> ownerUserId.equals(s.getSessionContext().getEntityId()))
        .forEach(s -> s.response(msg));
  }
}
