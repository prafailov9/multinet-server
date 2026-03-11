package com.ntros.persistence.repository;

import com.ntros.persistence.model.PlayerRecord;
import java.util.Optional;

/**
 * CRUD + statistics contract for the {@code players} table.
 *
 * <p>All methods are synchronous and intended to be called from a single thread
 * (the JVM shutdown hook or the session-lifecycle callback).
 */
public interface PlayerRepository {

  /**
   * Inserts the player if not present, otherwise updates {@code last_seen} to now.
   * Does NOT overwrite cumulative stats on an update — use
   * {@link #incrementMoves} / {@link #incrementSessions} for that.
   *
   * @param name player name
   * @return the persisted record (with database-assigned id)
   */
  PlayerRecord upsert(String name);

  /**
   * Returns the player record for {@code name}, if present.
   */
  Optional<PlayerRecord> findByName(String name);

  /**
   * Atomically increments {@code total_moves} by {@code delta} for the named player.
   * A player row must exist; call {@link #upsert} first if uncertain.
   *
   * @param name  player name
   * @param delta positive number of moves to add
   */
  void incrementMoves(String name, int delta);

  /**
   * Atomically increments {@code total_sessions} by 1 and sets {@code last_seen} to now.
   *
   * @param name player name
   */
  void recordSessionEnd(String name);
}
