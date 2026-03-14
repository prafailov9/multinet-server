package com.ntros.persistence.repository;

import com.ntros.persistence.model.InstanceRoleRecord;
import java.util.List;
import java.util.Optional;

/**
 * Manages the {@code clients_instance_roles} junction table.
 * <p>
 * One row represents a single client holding a specific role in a specific world. The schema
 * enforces {@code PRIMARY KEY (client_id, world_name)}, so each client has at most one role
 * per world at any given time.
 */
public interface ClientsInstanceRolesRepository {

  /** All role assignments across every client and every world. */
  List<InstanceRoleRecord> findAll();

  /** All world-role assignments for a single client, identified by username. */
  List<InstanceRoleRecord> findAllForClient(String username);

  /** All clients that hold a particular instance role (across all worlds). */
  List<InstanceRoleRecord> findAllForInstanceRole(String instanceRoleName);

  /**
   * The role a specific client holds in a specific world, if any.
   * Returns {@link Optional#empty()} when the client has never been assigned a role in that world.
   */
  Optional<InstanceRoleRecord> findRoleForClientInWorld(String username, String worldName);

  /**
   * Assigns {@code instanceRoleName} to {@code username} in {@code worldName}.
   * Upserts: if the client already has a role in that world it is replaced.
   */
  void assignRole(String username, String worldName, String instanceRoleName);

  /** Removes any role assignment for {@code username} in {@code worldName}. No-op if absent. */
  void removeRole(String username, String worldName);

}
