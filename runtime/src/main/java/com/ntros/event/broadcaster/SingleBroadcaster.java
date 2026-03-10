package com.ntros.event.broadcaster;

import com.ntros.event.sessionmanager.SessionManager;

/**
 * Broadcasts a binary frame to exactly one session — the session whose entity ID matches
 * {@link #ownerUserId}. Used for single-player / private worlds.
 */
public final class SingleBroadcaster implements Broadcaster {

  private final String ownerUserId;

  public SingleBroadcaster(String ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  @Override
  public void publish(byte[] frame, SessionManager sessions) {
    sessions.getActiveSessions().stream()
        .filter(s -> ownerUserId.equals(s.getSessionContext().getEntityId()))
        .forEach(s -> s.response(frame));
  }
}
