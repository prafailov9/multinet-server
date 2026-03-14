package com.ntros.lifecycle.session;


import com.ntros.model.entity.config.access.InstanceRole;
import com.ntros.model.entity.config.access.SystemRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Data;

/**
 * Represents session state information for a single client connection.
 * On creation, each session is of system_role = USER.
 * - system_role describes the session's authority on the system rather than a single instance.
 * - instance_roles describes the session's authority within an instance.
 */
@Data
public class SessionContext {

  // Transport/session-level
  private final long sessionId;            // immutable, set at session creation
  private final AtomicBoolean authenticated = new AtomicBoolean(false);
  private volatile String username;                   // set during AUTH (username/account)
  private volatile SystemRole systemRole;

  // World-level
  private volatile String worldId;                  // set during JOIN
  private volatile String entityId;                 // in-world entity (formerly playerId)
  // TODO: remove isntanceRole field
  private volatile InstanceRole instanceRole;
  private volatile List<InstanceRole> instanceRoles;
  private volatile OffsetDateTime joinedAt;

  public SessionContext(long sessionId) {
    this.sessionId = sessionId;
  }

  public SessionContext(long sessionId, SystemRole systemRole) {
    this.sessionId = sessionId;
    this.systemRole = systemRole;
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
