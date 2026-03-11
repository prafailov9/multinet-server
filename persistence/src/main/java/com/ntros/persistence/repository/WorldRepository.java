package com.ntros.persistence.repository;

import com.ntros.persistence.model.WorldRecord;
import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for world metadata ({@code worlds} table).
 *
 * <p>World rows are written once on first startup and never updated — they serve as a registry
 * of all worlds that have ever been configured on this server.
 */
public interface WorldRepository {

  /**
   * Inserts the world record if no row exists for {@code record.name()}.
   * Silently skips if already present (idempotent on repeated startups).
   *
   * @return the stored record
   */
  WorldRecord registerIfAbsent(WorldRecord record);

  /**
   * Looks up a world by its unique name.
   */
  Optional<WorldRecord> findByName(String worldName);

  /**
   * Returns all registered worlds, ordered by {@code created_at}.
   */
  List<WorldRecord> findAll();
}
