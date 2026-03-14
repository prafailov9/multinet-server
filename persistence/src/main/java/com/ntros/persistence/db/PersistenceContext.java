package com.ntros.persistence.db;

import com.ntros.persistence.repository.ClientRepository;
import com.ntros.persistence.repository.ClientsInstanceRolesRepository;
import com.ntros.persistence.repository.InstanceRoleRepository;
import com.ntros.persistence.repository.PlayerRepository;
import com.ntros.persistence.repository.SystemRoleRepository;
import com.ntros.persistence.repository.TerrainSnapshotRepository;
import com.ntros.persistence.repository.WorldRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Static registry for persistence repositories.
 *
 * <p>Follows the same convention as {@code Connectors} and {@code Instances} in the runtime
 * module: bootstrapped once at startup, then accessed via static getters anywhere in the process.
 *
 * <h3>Usage</h3>
 * <pre>
 * // In ServerBootstrap (once):
 * PersistenceContext.init(...);
 *
 * // Anywhere in the process:
 * PersistenceContext.clients().findByUsername("alice");
 * PersistenceContext.clientsInstanceRoles().findRoleForClientInWorld("alice", "arena-1");
 * </pre>
 */
@Slf4j
public final class PersistenceContext {

  // ── Repositories ────────────────────────────────────────────────────────────

  private static volatile ClientRepository clientRepository;
  private static volatile PlayerRepository playerRepository;
  private static volatile WorldRepository worldRepository;
  private static volatile TerrainSnapshotRepository terrainRepository;

  // Role repositories
  private static volatile SystemRoleRepository systemRoleRepository;
  private static volatile InstanceRoleRepository instanceRoleRepository;
  private static volatile ClientsInstanceRolesRepository clientsInstanceRolesRepository;

  private PersistenceContext() {
  }

  // ── Bootstrap ────────────────────────────────────────────────────────────────

  static synchronized void init(
      ClientRepository clients,
      PlayerRepository players,
      WorldRepository worlds,
      TerrainSnapshotRepository terrain,
      SystemRoleRepository systemRoles,
      InstanceRoleRepository instanceRoles,
      ClientsInstanceRolesRepository clientsInstanceRoles) {

    if (playerRepository != null) {
      throw new IllegalStateException("PersistenceContext already initialised.");
    }
    clientRepository = clients;
    playerRepository = players;
    worldRepository = worlds;
    terrainRepository = terrain;
    systemRoleRepository = systemRoles;
    instanceRoleRepository = instanceRoles;
    clientsInstanceRolesRepository = clientsInstanceRoles;
  }

  // ── Getters ──────────────────────────────────────────────────────────────────

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

  public static SystemRoleRepository systemRoles() {
    assertInit();
    return systemRoleRepository;
  }

  public static InstanceRoleRepository instanceRoles() {
    assertInit();
    return instanceRoleRepository;
  }

  public static ClientsInstanceRolesRepository clientsInstanceRoles() {
    assertInit();
    return clientsInstanceRolesRepository;
  }

  // ── Test support ─────────────────────────────────────────────────────────────

  /**
   * Resets the context — intended for use in tests only.
   */
  static synchronized void reset() {
    clientRepository = null;
    playerRepository = null;
    worldRepository = null;
    terrainRepository = null;
    systemRoleRepository = null;
    instanceRoleRepository = null;
    clientsInstanceRolesRepository = null;
  }

  // ── Internal ─────────────────────────────────────────────────────────────────

  private static void assertInit() {
    if (playerRepository == null) {
      throw new IllegalStateException(
          "PersistenceContext not initialised — call PersistenceContext.init(...) first.");
    }
  }
}
