package com.ntros.persistence.db;

import com.ntros.persistence.repository.impl.JsonTerrainSnapshotRepository;
import com.ntros.persistence.repository.impl.SqliteClientRepository;
import com.ntros.persistence.repository.impl.SqliteClientsInstanceRolesRepository;
import com.ntros.persistence.repository.impl.SqliteInstanceRoleRepository;
import com.ntros.persistence.repository.impl.SqlitePlayerRepository;
import com.ntros.persistence.repository.impl.SqliteSystemRoleRepository;
import com.ntros.persistence.repository.impl.SqliteWorldRepository;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DatabaseBuilder {

  // Drop in dependency order (FK children before parents)
  private static final String[] DROP_STATEMENTS = {
      "DROP TABLE IF EXISTS clients_instance_roles",
      "DROP TABLE IF EXISTS clients",
      "DROP TABLE IF EXISTS instance_roles",
      "DROP TABLE IF EXISTS system_roles",
      "DROP TABLE IF EXISTS players",
      "DROP TABLE IF EXISTS worlds",
      "DROP TABLE IF EXISTS sessions"
  };

  private DatabaseBuilder() {
  }

  /** Create with defaults. */
  public static void createDatabase() {
    init(ConnectionProvider.DEFAULT_DB_PATH, Path.of("data/snapshots"));
  }

  /** Create with optional paths. */
  public static void createDatabase(String dbPath, Path terrainPath) {
    init(dbPath, terrainPath);
  }

  public static void dropDatabase() {
    // Use the shared connection directly — do NOT close it here; that is ConnectionProvider's job.
    Connection conn = ConnectionProvider.connection();
    try (Statement stm = conn.createStatement()) {
      for (String sql : DROP_STATEMENTS) {
        stm.executeUpdate(sql);
      }
    } catch (SQLException ex) {
      log.error("[DatabaseBuilder] Error during dropDatabase: {}", ex.getMessage(), ex);
    }
    ConnectionProvider.close();
    PersistenceContext.reset();
  }

  public static void runScript(String sql) {
    try (Statement st = ConnectionProvider.connection().createStatement()) {
      st.executeUpdate(sql);
    } catch (SQLException e) {
      throw new RuntimeException("DatabaseBuilder.runScript failed: " + sql, e);
    }
  }

  // ── Internal ─────────────────────────────────────────────────────────────────

  private static void init(String dbPath, Path terrainPath) {
    log.info("[DatabaseBuilder] Starting ConnectionProvider and PersistenceContext init.");
    ConnectionProvider.initialize(dbPath);
    PersistenceContext.init(
        new SqliteClientRepository(),
        new SqlitePlayerRepository(),
        new SqliteWorldRepository(),
        new JsonTerrainSnapshotRepository(terrainPath),
        new SqliteSystemRoleRepository(),
        new SqliteInstanceRoleRepository(),
        new SqliteClientsInstanceRolesRepository()
    );
    log.info("[DatabaseBuilder] PersistenceContext ready.");
  }
}
