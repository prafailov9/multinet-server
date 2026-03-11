package com.ntros.persistence.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages a single SQLite JDBC connection for the lifetime of the server process.
 *
 * <p>SQLite supports only one writer at a time, so a single {@link Connection} shared across all
 * repositories is correct — and WAL mode ensures concurrent readers don't block it.
 *
 * <p>Call {@link #initialize(String)} once at server startup (before any repo is used), then
 * obtain the shared connection via {@link #get()}.
 */
@Slf4j
public final class ConnectionProvider {

  /** Default file path — relative to the working directory of the server process. */
  public static final String DEFAULT_DB_PATH = "data/multinet.db";

  private static volatile Connection connection;

  private ConnectionProvider() {}

  /**
   * Opens (or creates) the SQLite database at {@code dbPath}, enables WAL journal mode for
   * better concurrent read performance, and initialises the schema.
   *
   * @param dbPath file-system path to the SQLite database file (e.g. {@code "data/multinet.db"})
   * @throws IllegalStateException if called more than once
   */
  public static synchronized void initialize(String dbPath) {
    if (connection != null) {
      throw new IllegalStateException("ConnectionProvider already initialised.");
    }
    try {
      // Ensure the parent directory exists — skip for special in-memory paths (e.g. ":memory:")
      if (!dbPath.startsWith(":")) {
        java.nio.file.Path path = java.nio.file.Paths.get(dbPath);
        if (path.getParent() != null) {
          java.nio.file.Files.createDirectories(path.getParent());
        }
      }

      connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
      // WAL mode requires a file-based database; skip for in-memory ":memory:" databases
      if (!dbPath.startsWith(":")) {
        connection.createStatement().execute("PRAGMA journal_mode=WAL");
      }
      connection.setAutoCommit(true);

      log.info("[ConnectionProvider] SQLite database opened at '{}'.", dbPath);
      SchemaInitializer.run(connection);

    } catch (Exception e) {
      throw new RuntimeException("Failed to open SQLite database at: " + dbPath, e);
    }
  }

  /**
   * Returns the shared connection. Throws if {@link #initialize} has not been called.
   */
  public static Connection get() {
    if (connection == null) {
      throw new IllegalStateException(
          "ConnectionProvider not initialised — call initialize(dbPath) first.");
    }
    return connection;
  }

  /** Closes the connection. Safe to call multiple times. */
  public static synchronized void close() {
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

  /** Returns {@code true} if the connection has been initialised and is not closed. */
  public static boolean isOpen() {
    try {
      return connection != null && !connection.isClosed();
    } catch (SQLException e) {
      return false;
    }
  }
}
