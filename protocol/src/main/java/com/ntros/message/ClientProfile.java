package com.ntros.message;


import com.ntros.model.entity.config.access.Role;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientProfile {

  // Transport/session-level
  private final long sessionId;            // immutable, set at session creation
  private final AtomicBoolean authenticated = new AtomicBoolean(false);
  private String userId;                   // set during AUTH (username/account)

  // World-level
  private String worldId;                  // set during JOIN
  private String entityId;                 // in-world entity (formerly playerId)
  private Role role;                       // PLAYER, ORCHESTRATOR, OBSERVER
  private OffsetDateTime joinedAt;

  public ClientProfile(long sessionId) {
    this.sessionId = sessionId;
  }

  // Getters/setters

  public long getSessionId() {
    return sessionId;
  }

  public boolean isAuthenticated() {
    return authenticated.get();
  }

  public void setAuthenticated(boolean value) {
    authenticated.set(value);
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getWorldId() {
    return worldId;
  }

  public void setWorldId(String worldId) {
    this.worldId = worldId;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public OffsetDateTime getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(OffsetDateTime joinedAt) {
    this.joinedAt = joinedAt;
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
