package com.ntros.persistence.db;

import com.ntros.persistence.exception.SqlCrudException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;

/**
 * Test-only helper for managing the in-memory SQLite database lifecycle and running
 * arbitrary SQL scripts (e.g. to clear table data between subtests).
 *
 * Use {@link #runScript} when a test needs to clear one or more tables mid-test.
 */
@Slf4j
public final class TestDbHelper {

  private TestDbHelper() {
  }

  /**
   * Opens a fresh in-memory SQLite database and initialises the schema.
   */
  public static void createDb() {
    ConnectionProvider.initialize(":memory:");
  }

  public static void dropDatabase() {
    try (Connection conn = ConnectionProvider.connection()) {
      try (Statement stm = conn.createStatement()) {
        stm.executeUpdate(
            "drop table clients; drop table players; drop table worlds; drop table sessions");
      } catch (SQLException ex) {
        log.error("Error during drop database: {}", ex.getMessage(), ex);
      }
    } catch (SQLException ex) {
      log.error("Could not get connection: {}", ex.getMessage(), ex);
      throw new SqlCrudException("Could not get connection", ex);
    }
  }

  /**
   * Executes a single SQL statement against the shared test connection.
   *
   * <p>Typical usage:
   * <pre>
   * TestDbHelper.runScript("DELETE FROM players");
   * TestDbHelper.runScript("DELETE FROM worlds");
   * </pre>
   *
   * @param sql any valid SQL statement (DDL or DML)
   * @throws RuntimeException wrapping the underlying {@link SQLException} on failure
   */
  public static void runScript(String sql) {
    try (Statement st = ConnectionProvider.connection().createStatement()) {
      st.executeUpdate(sql);
    } catch (SQLException e) {
      throw new RuntimeException("TestDbHelper.runScript failed for: " + sql, e);
    }
  }
}
