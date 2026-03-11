package com.ntros.persistence;

import com.ntros.persistence.repository.PlayerRepository;
import com.ntros.persistence.repository.TerrainSnapshotRepository;
import com.ntros.persistence.repository.WorldRepository;

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
public final class PersistenceContext {

  private static volatile PlayerRepository          playerRepository;
  private static volatile WorldRepository           worldRepository;
  private static volatile TerrainSnapshotRepository terrainRepository;

  private PersistenceContext() {}

  /**
   * Initialises the context with all three repositories. Must be called exactly once during
   * server startup, before any command or hook tries to access a repository.
   *
   * @throws IllegalStateException if called more than once
   */
  public static synchronized void init(
      PlayerRepository          players,
      WorldRepository           worlds,
      TerrainSnapshotRepository terrain) {

    if (playerRepository != null) {
      throw new IllegalStateException("PersistenceContext already initialised.");
    }
    playerRepository  = players;
    worldRepository   = worlds;
    terrainRepository = terrain;
  }

  /** Returns the player repository. Throws if not initialised. */
  public static PlayerRepository players() {
    assertInit();
    return playerRepository;
  }

  /** Returns the world registry repository. Throws if not initialised. */
  public static WorldRepository worlds() {
    assertInit();
    return worldRepository;
  }

  /** Returns the terrain snapshot repository. Throws if not initialised. */
  public static TerrainSnapshotRepository terrain() {
    assertInit();
    return terrainRepository;
  }

  /** Returns {@code true} once {@link #init} has been called. */
  public static boolean isInitialised() {
    return playerRepository != null;
  }

  /** Resets the context — intended for use in tests only. */
  public static synchronized void reset() {
    playerRepository  = null;
    worldRepository   = null;
    terrainRepository = null;
  }

  private static void assertInit() {
    if (playerRepository == null) {
      throw new IllegalStateException(
          "PersistenceContext not initialised — call PersistenceContext.init(...) first.");
    }
  }
}
