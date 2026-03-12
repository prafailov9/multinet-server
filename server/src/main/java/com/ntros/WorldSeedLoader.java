package com.ntros;

import com.ntros.persistence.db.PersistenceContext;
import com.ntros.persistence.model.WorldRecord;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds the default world definitions into the {@code worlds} table on first startup.
 *
 * <p>All world configuration lives in the database — this class is the single place where the
 * defaults are declared. After first startup they can be modified directly in the DB.
 *
 * <p>The seed is idempotent: if the table already contains at least one row the method returns
 * immediately without touching the database.
 */
@Slf4j
public final class WorldSeedLoader {

  /**
   * Default worlds. Only 2-D worlds are included — 3-D / 4-D worlds are not yet supported by the
   * runtime and are skipped.
   */
  static final List<WorldRecord> DEFAULT_WORLDS = List.of(
      world("arena-x-multi", "GRID", 9, 12, true, true, false, true),
      world("arena-y-multi", "GRID", 7, 7, true, true, false, true),
      world("gol-small", "GOL", 18, 18, false, true, true, true),
      world("gol-mid", "GOL", 256, 256, false, true, true, true),
      world("gol-big", "GOL", 1024, 1024, false, true, true, true)
  );

  private WorldSeedLoader() {
  }

  /**
   * Inserts all {@link #DEFAULT_WORLDS} into the DB if the table is empty; otherwise no-op.
   */
  public static void seedIfEmpty() {
    List<WorldRecord> existing = PersistenceContext.worlds().findAll();
    if (!existing.isEmpty()) {
      log.info("[WorldSeedLoader] {} world(s) already in DB — skipping seed.", existing.size());
      return;
    }
    for (WorldRecord record : DEFAULT_WORLDS) {
      PersistenceContext.worlds().registerIfAbsent(record);
    }
    log.info("[WorldSeedLoader] Seeded {} default world(s) into DB.", DEFAULT_WORLDS.size());
  }

  // ── Factory helper ────────────────────────────────────────────────────────

  private static WorldRecord world(String name, String engineType,
      int width, int height,
      boolean multiplayer, boolean orchestrated,
      boolean hasAi, boolean deterministic) {
    return new WorldRecord(name, engineType, width, height, 0,
        multiplayer, orchestrated, hasAi, deterministic, Instant.now());
  }
}
