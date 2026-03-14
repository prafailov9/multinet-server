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
 * system_roles           — master list of server-wide privilege levels (USER / SUPERUSER / ROOT)
 * instance_roles         — master list of world-scoped privilege levels (OBSERVER .. OWNER)
 * clients                — one row per registered account; holds a FK to system_roles
 * clients_instance_roles — one row per (client × world); records the role a client holds in a
 *                          specific world instance; enforces at most one role per client per world
 * players                — one row per unique player name; accumulates lifetime stats
 * worlds                 — one row per registered world (metadata only, no game state)
 * sessions               — append-only session history for auditing
 * </pre>
 */
@Slf4j
final class SchemaInitializer {

  private SchemaInitializer() {
  }

  static void run(Connection conn) throws SQLException {
    try (Statement st = conn.createStatement()) {

      // ── Reference tables ───────────────────────────────────────────────────

      st.execute("""
          CREATE TABLE IF NOT EXISTS system_roles (
            system_role_id   INTEGER PRIMARY KEY AUTOINCREMENT,
            system_role_name TEXT    NOT NULL UNIQUE
          )
          """);

      st.execute("""
          CREATE TABLE IF NOT EXISTS instance_roles (
            instance_role_id   INTEGER PRIMARY KEY AUTOINCREMENT,
            instance_role_name TEXT    NOT NULL UNIQUE
          )
          """);

      // ── Seed reference data (idempotent) ───────────────────────────────────
      // Values mirror the SystemRole and InstanceRole enums in the domain module.
      // INSERT OR IGNORE keeps this safe to re-run without duplicating rows.

      st.execute("""
          INSERT OR IGNORE INTO system_roles (system_role_name)
          VALUES ('USER'), ('SUPERUSER'), ('ROOT')
          """);

      st.execute("""
          INSERT OR IGNORE INTO instance_roles (instance_role_name)
          VALUES ('OBSERVER'), ('PLAYER'), ('GAME_MASTER'), ('MODERATOR'), ('ADMIN'), ('OWNER')
          """);

      // ── Client / account table ─────────────────────────────────────────────
      // system_role_id defaults to the USER row (looked up at INSERT time).

      st.execute("""
          CREATE TABLE IF NOT EXISTS clients (
            client_id      INTEGER PRIMARY KEY AUTOINCREMENT,
            system_role_id INTEGER NOT NULL REFERENCES system_roles(system_role_id),
            session_id     INTEGER NOT NULL DEFAULT 0,
            username       TEXT    NOT NULL UNIQUE,
            password       TEXT,
            created_at     TEXT,
            updated_at     TEXT
          )
          """);

      st.execute("""
          CREATE UNIQUE INDEX IF NOT EXISTS idx_cl_uname ON clients(username)
          """);

      // ── Per-world role assignments ─────────────────────────────────────────
      // One row = one client holding one role in one world.
      // PRIMARY KEY (client_id, world_name) enforces at most one role per client per world —
      // a client cannot simultaneously be ADMIN and PLAYER in the same world.

      st.execute("""
          CREATE TABLE IF NOT EXISTS clients_instance_roles (
            client_id        INTEGER NOT NULL REFERENCES clients(client_id),
            world_name       TEXT    NOT NULL,
            instance_role_id INTEGER NOT NULL REFERENCES instance_roles(instance_role_id),
            PRIMARY KEY (client_id, world_name)
          )
          """);

      // ── Player lifetime stats ──────────────────────────────────────────────

      st.execute("""
          CREATE TABLE IF NOT EXISTS players (
            id               INTEGER PRIMARY KEY,
            name             TEXT    NOT NULL UNIQUE,
            total_moves      INTEGER NOT NULL DEFAULT 0,
            total_sessions   INTEGER NOT NULL DEFAULT 0,
            last_seen        TEXT
          )
          """);

      // ── World metadata ─────────────────────────────────────────────────────
      // Worlds are always re-seeded from WorldSeedLoader at startup, so drop-and-recreate
      // is safe and guarantees the schema stays current after any upgrade.

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

      // ── Session audit log ──────────────────────────────────────────────────

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
