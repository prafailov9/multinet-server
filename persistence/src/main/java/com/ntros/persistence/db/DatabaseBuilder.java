package com.ntros.persistence.db;

import com.ntros.persistence.exception.SqlCrudException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DatabaseBuilder {

  private DatabaseBuilder() {
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


  public static void runScript(String sql) {
    try (Statement st = ConnectionProvider.connection().createStatement()) {
      st.executeUpdate(sql);
    } catch (SQLException e) {
      throw new RuntimeException("TestDbHelper.runScript failed for: " + sql, e);
    }
  }
}