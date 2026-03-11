package com.ntros.persistence.db;

import com.ntros.persistence.repository.ClientRepository;
import com.ntros.persistence.repository.PlayerRepository;
import com.ntros.persistence.repository.TerrainSnapshotRepository;
import com.ntros.persistence.repository.WorldRepository;
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

  static synchronized void init(ClientRepository clients,
      PlayerRepository players,
      WorldRepository worlds,
      TerrainSnapshotRepository terrain) {

    if (playerRepository != null) {
      throw new IllegalStateException("PersistenceContext already initialised.");
    }
    clientRepository = clients;
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

  /**
   * Resets the context — intended for use in tests only.
   */
  static synchronized void reset() {
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
