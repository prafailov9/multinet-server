package com.ntros.event.broadcaster;

import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.model.world.protocol.encoder.StateFrame;

public final class BroadcastToOwnerOnly implements Broadcaster {

  private final String ownerUserId;

  public BroadcastToOwnerOnly(String ownerUserId) {
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
