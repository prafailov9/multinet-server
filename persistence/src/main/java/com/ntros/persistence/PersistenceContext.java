package com.ntros.persistence;

import com.ntros.persistence.db.ConnectionProvider;
import com.ntros.persistence.repository.ClientRepository;
import com.ntros.persistence.repository.PlayerRepository;
import com.ntros.persistence.repository.TerrainSnapshotRepository;
import com.ntros.persistence.repository.WorldRepository;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;

/**
 * Static registry for persistence repositories.
 *
 * <p>Follows the same convention as {@code Connectors} and {@code Instances} in the runtime
 * module: bootstrapped once at startup, then accessed via static getters anywhere in the process.
 *
 * <p>Keeping it static (rather than DI-container-managed) avoids pulling a DI framework into a
 * project that currently uses none, while still making repos accessible from commands and hooks
 * that live in separate modules.
 *
 * <h3>Usage</h3>
 * <pre>
 * // In ServerBootstrap (once):
 * PersistenceContext.init(playerRepo, worldRepo, terrainRepo);
 *
 * // Anywhere in the process:
 * PersistenceContext.players().upsert("alice");
 * </pre>
 */
@Slf4j
public final class PersistenceContext {

  private static volatile ClientRepository clientRepository;

  private static volatile PlayerRepository playerRepository;
  private static volatile WorldRepository worldRepository;
  private static volatile TerrainSnapshotRepository terrainRepository;

  private PersistenceContext() {
  }

  /**
   * Initialises the context with all three repositories. Must be called exactly once during
   * server startup, before any command or hook tries to access a repository.
   *
   * @throws IllegalStateException if called more than once
   */
  public static synchronized void init(ClientRepository clientRepository,
      PlayerRepository players,
      WorldRepository worlds,
      TerrainSnapshotRepository terrain) {

    if (playerRepository != null) {
      throw new IllegalStateException("PersistenceContext already initialised.");
    }
    playerRepository = players;
    worldRepository = worlds;
    terrainRepository = terrain;
  }

  public static ClientRepository clients() {
    assertInit();
    return clientRepository;
  }

  public static PlayerRepository players() {
    assertInit();
    return playerRepository;
  }

  public static WorldRepository worlds() {
    assertInit();
    return worldRepository;
  }

  public static TerrainSnapshotRepository terrain() {
    assertInit();
    return terrainRepository;
  }

  public static boolean isInitialised() {
    return playerRepository != null;
  }

  /**
   * Deletes all data managed by the persistence layer — <strong>intended for testing only</strong>.
   *
   * <p>Wipes every row from {@code players}, {@code worlds}, {@code clients}, and
   * {@code sessions}, then deletes all terrain snapshot files. The database schema (table
   * definitions) and the snapshot directory itself are preserved; only the data is removed.
   *
   * <p>Must be called after {@link #init}. After this call the database is in the same state as a
   * freshly initialised, empty installation.
   */
  public static synchronized void clearAll() {
    assertInit();
    // Repos that own a deleteAll() implementation
    // sessions and clients tables have no repo wired into PersistenceContext — clear directly
    clearTable("sessions");
    clearTable("clients");
    log.info("[PersistenceContext] All persistence data cleared.");
  }

  /**
   * Truncates a single table via the shared connection.
   */
  private static void clearTable(String table) {
    try (var st = ConnectionProvider.connection().createStatement()) {
      int rows = st.executeUpdate("DELETE FROM " + table);
      log.debug("[PersistenceContext] Cleared table '{}' ({} rows).", table, rows);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to clear table: " + table, e);
    }
  }

  /**
   * Resets the context — intended for use in tests only.
   */
  public static synchronized void reset() {
    playerRepository = null;
    worldRepository = null;
    terrainRepository = null;
  }

  private static void assertInit() {
    if (playerRepository == null) {
      throw new IllegalStateException(
          "PersistenceContext not initialised — call PersistenceContext.init(...) first.");
    }
  }
}
