package com.ntros.persistence.db;

import com.ntros.persistence.exception.SqlCrudException;
import com.ntros.persistence.repository.impl.JsonTerrainSnapshotRepository;
import com.ntros.persistence.repository.impl.SqliteClientRepository;
import com.ntros.persistence.repository.impl.SqlitePlayerRepository;
import com.ntros.persistence.repository.impl.SqliteWorldRepository;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DatabaseBuilder {

  private static final String DROP_ALL_QUERY = "drop table clients; drop table players; drop table worlds; drop table sessions";


  private DatabaseBuilder() {
  }

  /**
   * Create with defaults
   */
  public static void createDatabase() {
    init(ConnectionProvider.DEFAULT_DB_PATH, Path.of("data/snapshots"));
  }

  /**
   * create with optional paths
   */
  public static void createDatabase(String dbPath, Path terrainPath) {
    init(dbPath, terrainPath);
  }

  public static void dropDatabase() {
    // Use the shared connection directly — do NOT use try-with-resources here,
    // because closing the shared connection is ConnectionProvider.close()'s job.
    Connection conn = ConnectionProvider.connection();
    try (Statement stm = conn.createStatement()) {
      stm.executeUpdate(DROP_ALL_QUERY);
    } catch (SQLException ex) {
      log.error("Error during drop database: {}", ex.getMessage(), ex);
    }
    ConnectionProvider.close();
    PersistenceContext.reset();
  }

  public static void runScript(String sql) {
    try (Statement st = ConnectionProvider.connection().createStatement()) {
      st.executeUpdate(sql);
    } catch (SQLException e) {
      throw new RuntimeException("TestDbHelper.runScript failed for: " + sql, e);
    }
  }

  private static void init(String dbPath, Path terrainPath) {
    log.info("[DatabaseBuilder]: started ConnectionProvider and PersistenceContext init.");
    ConnectionProvider.initialize(dbPath);
    PersistenceContext.init(new SqliteClientRepository(), new SqlitePlayerRepository(),
        new SqliteWorldRepository(), new JsonTerrainSnapshotRepository(terrainPath));
  }

}