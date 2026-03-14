package com.ntros.persistence.model;



import com.ntros.model.entity.config.access.SystemRole;
import java.time.Instant;

public record ClientRecord(
    long clientId,
    long sessionId,
    String username,
    String password,
    String role,
    Instant createdAt,
    Instant updatedAt
) {

  /**
   * Convenience factory for a brand-new player (before any DB id is assigned).
   */
  public static ClientRecord newClient(String username, String password, long sessionId) {
    return new ClientRecord(0L, sessionId, username, password, SystemRole.USER.name(), Instant.now(),
        Instant.now());
  }

  public static ClientRecord newClient(String username, String password, long sessionId,
      String role) {
    return new ClientRecord(0L, sessionId, username, password, role, Instant.now(), Instant.now());
  }

}