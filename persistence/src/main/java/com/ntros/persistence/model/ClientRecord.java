package com.ntros.persistence.model;


import static com.ntros.persistence.model.Role.USER;

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
    return new ClientRecord(0L, sessionId, username, password, USER.toString(), Instant.now(),
        Instant.now());
  }

  public static ClientRecord newClient(String username, String password, long sessionId,
      String role) {
    return new ClientRecord(0L, sessionId, username, password, role, Instant.now(), Instant.now());
  }

}