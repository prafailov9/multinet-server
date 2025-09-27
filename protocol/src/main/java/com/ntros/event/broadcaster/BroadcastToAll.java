package com.ntros.event.broadcaster;

import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.session.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BroadcastToAll implements Broadcaster {

  /**
   * When publish() fails to send to a session, remove it from the SessionManager right there
   * (best-effort), so you don’t keep trying every tick.
   */
  @Override
  public void publish(String payload, SessionManager sessions) {
    for (Session s : sessions.getActiveSessions()) {
      try {
        s.response(payload);
      } catch (Exception ex) {
        log.warn("broadcast failed, detaching session {}: {}",
            s.getSessionContext().getSessionId(), ex.toString());
        sessions.remove(s);
      }
    }
  }
}
