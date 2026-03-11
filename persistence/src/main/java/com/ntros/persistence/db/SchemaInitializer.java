package com.ntros.persistence.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates the database schema on first startup. All statements use {@code IF NOT EXISTS} so they
 * are safe to re-run on every startup without data loss.
 *
 * <h3>Schema</h3>
 * <pre>
 * players  — one row per unique player name, accumulates lifetime stats
 * worlds   — one row per registered world (metadata only, no game state)
 * sessions — append-only session history for auditing and stat calculation
 * </pre>
 */
@Slf4j
final class SchemaInitializer {

  private SchemaInitializer() {}

  static void run(Connection conn) throws SQLException {
    try (Statement st = conn.createStatement()) {
      // ── players ────────────────────────────────────────────────────────────
      st.execute("""
          CREATE TABLE IF NOT EXISTS players (
            id               INTEGER PRIMARY KEY,
            name             TEXT    NOT NULL UNIQUE,
            total_moves      INTEGER NOT NULL DEFAULT 0,
            total_sessions   INTEGER NOT NULL DEFAULT 0,
            last_seen        TEXT
          )
          """);

      // ── worlds ─────────────────────────────────────────────────────────────
      st.execute("""
          CREATE TABLE IF NOT EXISTS worlds (
            name       TEXT    PRIMARY KEY,
            type       TEXT    NOT NULL,
            width      INTEGER NOT NULL,
            height     INTEGER NOT NULL,
            depth      INTEGER NOT NULL DEFAULT 0,
            created_at TEXT    NOT NULL
          )
          """);

      // ── sessions ───────────────────────────────────────────────────────────
      st.execute("""
          CREATE TABLE IF NOT EXISTS sessions (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            player_name TEXT    NOT NULL,
            world_name  TEXT    NOT NULL,
            joined_at   TEXT    NOT NULL,
            left_at     TEXT
          )
          """);

      log.info("[SchemaInitializer] Schema ready.");
    }
  }
}
