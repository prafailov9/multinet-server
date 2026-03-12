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

  private SchemaInitializer() {
  }

  static void run(Connection conn) throws SQLException {
    try (Statement st = conn.createStatement()) {
      st.execute("""
          CREATE TABLE IF NOT EXISTS clients (
            client_id     INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id     INTEGER NOT NULL DEFAULT 0,
            username      TEXT NOT NULL UNIQUE,
            password      TEXT,
            client_role   TEXT,
            created_at    TEXT,
            updated_at    TEXT
          )
          """);

      st.execute("""
          CREATE TABLE IF NOT EXISTS players (
            id               INTEGER PRIMARY KEY,
            name             TEXT    NOT NULL UNIQUE,
            total_moves      INTEGER NOT NULL DEFAULT 0,
            total_sessions   INTEGER NOT NULL DEFAULT 0,
            last_seen        TEXT
          )
          """);

      // Worlds are always seeded from WorldSeedLoader at startup, so we can safely
      // drop-and-recreate to guarantee the schema is up to date after any crash or upgrade.
      st.execute("DROP TABLE IF EXISTS worlds");
      st.execute("""
          CREATE TABLE worlds (
            name          TEXT    PRIMARY KEY,
            engine_type   TEXT    NOT NULL DEFAULT 'GRID',
            width         INTEGER NOT NULL DEFAULT 0,
            height        INTEGER NOT NULL DEFAULT 0,
            depth         INTEGER NOT NULL DEFAULT 0,
            multiplayer   INTEGER NOT NULL DEFAULT 0,
            orchestrated  INTEGER NOT NULL DEFAULT 0,
            has_ai        INTEGER NOT NULL DEFAULT 0,
            deterministic INTEGER NOT NULL DEFAULT 0,
            created_at    TEXT    NOT NULL
          )
          """);

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
