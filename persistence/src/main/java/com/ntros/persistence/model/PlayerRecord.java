package com.ntros.persistence.model;

import java.time.Instant;

/**
 * Lightweight DTO that mirrors the {@code players} database row.
 *
 * @param id             surrogate key (auto-assigned by the database)
 * @param name           unique player name
 * @param totalMoves     lifetime move count
 * @param totalSessions  lifetime session count
 * @param lastSeen       wall-clock time of the most recent disconnect, or {@code null} if unseen
 */
public record PlayerRecord(
    long    id,
    String  name,
    int     totalMoves,
    int     totalSessions,
    Instant lastSeen
) {

  /** Convenience factory for a brand-new player (before any DB id is assigned). */
  public static PlayerRecord newPlayer(String name) {
    return new PlayerRecord(0L, name, 0, 0, null);
  }
}
