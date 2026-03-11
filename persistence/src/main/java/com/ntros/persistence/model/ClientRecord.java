package com.ntros.persistence.model;

import java.time.Instant;

public record ClientRecord(
    long clientId,
    long sessionId,
    String username,
    String password,
    Instant createdAt,
    Instant updatedAt
) {

  /**
   * Convenience factory for a brand-new player (before any DB id is assigned).
   */
  public static ClientRecord newClient(String username, String password, long sessionId) {
    return new ClientRecord(0L, sessionId, username, password, Instant.now(), Instant.now());
  }
}