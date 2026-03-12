package com.ntros.persistence.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages a single SQLite JDBC connection for the lifetime of the server process.
 *
 * <p>SQLite supports only one writer at a time, so a single {@link Connection} shared across all
 * repositories is correct — and WAL mode ensures concurrent readers don't block it.
 *
 * <p>Call {@link #initialize(String)} once at server startup (before any repo is used), then
 * obtain the shared connection via {@link #connection()}.
 */
@Slf4j
public final class ConnectionProvider {

  /**
   * Default file path — relative to the working directory of the server process.
   */
  public static final String DEFAULT_DB_PATH = "data/multinet.db";

  private static volatile Connection connection;
  private static final AtomicInteger counter = new AtomicInteger(0);

  private ConnectionProvider() {
  }

  /**
   * Opens (or creates) the SQLite database at {@code dbPath}, enables WAL journal mode for
   * better concurrent read performance, and initialises the schema.
   *
   * @param dbPath file-system path to the SQLite database file (e.g. {@code "data/multinet.db"})
   * @throws IllegalStateException if called more than once
   */
  static synchronized void initialize(String dbPath) {
    log.info("[ConnectionProvider]: init started. Attempt: {}", counter.incrementAndGet());
    if (connection != null) {
      throw new IllegalStateException("ConnectionProvider already initialised.");
    }
    try {
      // Ensure the parent directory exists — skip for special in-memory paths (e.g. ":memory:")
      if (!dbPath.startsWith(":")) {
        Path path = Paths.get(dbPath);
        if (path.getParent() != null) {
          Files.createDirectories(path.getParent());
        }
      }

      connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
      log.info("[ConnectionProvider] SQLite database opened at '{}'.", dbPath);

      try (Statement st = connection.createStatement()) {
        // Give concurrent readers / stale WAL writers up to 5 s to release locks
        // before throwing SQLITE_BUSY — prevents startup failures after a crash.
        st.execute("PRAGMA busy_timeout=5000");

        // WAL mode: better read concurrency on file-based databases.
        // Skip for in-memory databases (:memory:) which don't support WAL.
        // Must come BEFORE any write (including schema init) so the journal
        // mode is active for every subsequent statement.
        // NOTE: do NOT call connection.setAutoCommit(true) after this — JDBC
        // connections are autocommit by default, and calling it explicitly on
        // a WAL-mode connection triggers an internal checkpoint that can fail
        // with SQLITE_BUSY if a stale WAL file exists from a previous crash.
        if (!dbPath.startsWith(":")) {
          st.execute("PRAGMA journal_mode=WAL");
        }
      }

      SchemaInitializer.run(connection);

    } catch (Exception e) {
      throw new RuntimeException("Failed to open SQLite database at: " + dbPath, e);
    }
  }

  /**
   * Returns the shared connection. Throws if {@link #initialize} has not been called.
   */
  public static Connection connection() {
    if (connection == null) {
      throw new IllegalStateException(
          "ConnectionProvider not initialised — call initialize(dbPath) first.");
    }
    return connection;
  }

  /**
   * Closes the connection. Safe to call multiple times.
   */
  static synchronized void close() {
    if (connection != null) {
      try {
        connection.close();
        log.info("[ConnectionProvider] SQLite connection closed.");
      } catch (SQLException e) {
        log.warn("[ConnectionProvider] Error closing connection: {}", e.getMessage());
      } finally {
        connection = null;
      }
    }
  }

  /**
   * Returns {@code true} if the connection has been initialized and is not closed.
   */
  public static boolean isOpen() {
    try {
      return connection != null && !connection.isClosed();
    } catch (SQLException e) {
      return false;
    }
  }
}
