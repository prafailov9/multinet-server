package com.ntros.event.broadcaster;

import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.lifecycle.session.Session;
import lombok.extern.slf4j.Slf4j;

/**
 * Broadcasts a binary frame to every active session.
 *
 * <p>If delivery to a session throws, that session is detached immediately so that a single
 * broken connection does not block or poison subsequent deliveries.
 */
@Slf4j
public final class SessionsBroadcaster implements Broadcaster {

  @Override
  public void publish(byte[] frame, SessionManager sessions) {
    for (Session s : sessions.getActiveSessions()) {
      try {
        s.response(frame);
      } catch (Exception ex) {
        log.warn("broadcast failed, detaching session {}: {}",
            s.getSessionContext().getSessionId(), ex.toString());
        sessions.remove(s);
      }
    }
  }
}
