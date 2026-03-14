package com.ntros.persistence.model;


import com.ntros.model.entity.config.access.SystemRole;
import java.time.Instant;
import java.util.List;

public record ClientRecord(
    long clientId,
    long sessionId,
    SystemRoleRecord systemRoleRecord,
    List<InstanceRoleRecord> instanceRoleRecords,
    String username,
    String password,
    Instant createdAt,
    Instant updatedAt
) {

  /**
   * Convenience factory for a brand-new player (before any DB id is assigned).
   */
  public static ClientRecord newClient(String username, String password, long sessionId) {
    return new ClientRecord(0L, sessionId, new SystemRoleRecord(SystemRole.USER.name()), List.of(),
        username, password, Instant.now(),
        Instant.now());
  }
}