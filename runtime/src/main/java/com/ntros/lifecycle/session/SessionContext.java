package com.ntros.lifecycle.session;


import com.ntros.model.entity.config.access.Role;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Data;

@Data
public class SessionContext {

  // Transport/session-level
  private final long sessionId;            // immutable, set at session creation
  private final AtomicBoolean authenticated = new AtomicBoolean(false);
  private volatile String username;                   // set during AUTH (username/account)

  // World-level
  private volatile String worldId;                  // set during JOIN
  private volatile String entityId;                 // in-world entity (formerly playerId)
  private volatile Role role;                       // PLAYER, ORCHESTRATOR, OBSERVER
  private volatile OffsetDateTime joinedAt;

  public SessionContext(long sessionId) {
    this.sessionId = sessionId;
  }

  public boolean isAuthenticated() {
    return authenticated.get();
  }

  public void setAuthenticated(boolean value) {
    authenticated.set(value);
  }

  public String getWorldName() {
    return worldId;
  }

  @Override
  public String toString() {
    return "ProtocolContext{" +
        "sessionId='" + sessionId + '\'' +
        ", worldId='" + worldId + '\'' +
        ", joinedAt=" + joinedAt +
        ", isAuthenticated=" + authenticated.get() +
        '}';
  }
}
