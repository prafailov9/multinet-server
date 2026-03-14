package com.ntros.persistence.repository.impl;

import static com.ntros.persistence.db.ConnectionProvider.connection;

import com.ntros.persistence.exception.SqlCrudException;
import com.ntros.persistence.model.InstanceRoleRecord;
import com.ntros.persistence.repository.ClientsInstanceRolesRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * SQLite-backed implementation of {@link ClientsInstanceRolesRepository}.
 *
 * <p>All queries go through the {@code clients_instance_roles} junction table joined with
 * {@code instance_roles} (for the role name) and {@code clients} (to resolve usernames to
 * client_id without callers having to manage IDs directly).
 *
 * <p>The schema enforces {@code PRIMARY KEY (client_id, world_name)}, guaranteeing at most one
 * role per client per world. {@link #assignRole} therefore uses {@code INSERT OR REPLACE} to
 * allow safe re-assignment without a separate check-then-update round-trip.
 */
@Slf4j
public class SqliteClientsInstanceRolesRepository implements ClientsInstanceRolesRepository {

  // Returns all (username, world_name, instance_role_id, instance_role_name) rows
  private static final String FIND_ALL_QUERY = """
      SELECT c.username, cir.world_name, ir.instance_role_id, ir.instance_role_name
      FROM clients_instance_roles cir
      JOIN clients       c  ON cir.client_id        = c.client_id
      JOIN instance_roles ir ON cir.instance_role_id = ir.instance_role_id
      ORDER BY c.username, cir.world_name
      """;

  // All world-role pairs for a given username
  private static final String FIND_ALL_FOR_CLIENT_QUERY = """
      SELECT cir.world_name, ir.instance_role_id, ir.instance_role_name
      FROM clients_instance_roles cir
      JOIN clients        c  ON cir.client_id        = c.client_id
      JOIN instance_roles ir ON cir.instance_role_id = ir.instance_role_id
      WHERE c.username = ?
      ORDER BY cir.world_name
      """;

  // All clients that hold a specific role (any world)
  private static final String FIND_ALL_FOR_ROLE_QUERY = """
      SELECT c.username, cir.world_name, ir.instance_role_id, ir.instance_role_name
      FROM clients_instance_roles cir
      JOIN clients        c  ON cir.client_id        = c.client_id
      JOIN instance_roles ir ON cir.instance_role_id = ir.instance_role_id
      WHERE ir.instance_role_name = ?
      ORDER BY c.username
      """;

  // Single role assignment for one client in one world
  private static final String FIND_FOR_CLIENT_IN_WORLD_QUERY = """
      SELECT cir.world_name, ir.instance_role_id, ir.instance_role_name
      FROM clients_instance_roles cir
      JOIN clients        c  ON cir.client_id        = c.client_id
      JOIN instance_roles ir ON cir.instance_role_id = ir.instance_role_id
      WHERE c.username = ? AND cir.world_name = ?
      """;

  // Upsert: safe to call even when a row already exists (replaces on conflict)
  private static final String ASSIGN_ROLE_QUERY = """
      INSERT OR REPLACE INTO clients_instance_roles (client_id, world_name, instance_role_id)
      VALUES (
        (SELECT client_id FROM clients WHERE username = ?),
        ?,
        (SELECT instance_role_id FROM instance_roles WHERE instance_role_name = ?)
      )
      """;

  private static final String REMOVE_ROLE_QUERY = """
      DELETE FROM clients_instance_roles
      WHERE client_id = (SELECT client_id FROM clients WHERE username = ?)
        AND world_name = ?
      """;

  // ── Public API ──────────────────────────────────────────────────────────────

  @Override
  public List<InstanceRoleRecord> findAll() {
    List<InstanceRoleRecord> results = new ArrayList<>();
    try (PreparedStatement ps = connection().prepareStatement(FIND_ALL_QUERY);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        results.add(mapWithWorld(rs));
      }
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to list all client instance roles", ex);
    }
    return results;
  }

  @Override
  public List<InstanceRoleRecord> findAllForClient(String username) {
    List<InstanceRoleRecord> results = new ArrayList<>();
    try (PreparedStatement ps = connection().prepareStatement(FIND_ALL_FOR_CLIENT_QUERY)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          results.add(mapWithWorld(rs));
        }
      }
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to list instance roles for client: " + username, ex);
    }
    return results;
  }

  @Override
  public List<InstanceRoleRecord> findAllForInstanceRole(String instanceRoleName) {
    List<InstanceRoleRecord> results = new ArrayList<>();
    try (PreparedStatement ps = connection().prepareStatement(FIND_ALL_FOR_ROLE_QUERY)) {
      ps.setString(1, instanceRoleName);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          results.add(mapWithWorld(rs));
        }
      }
    } catch (SQLException ex) {
      throw new SqlCrudException(
          "Failed to list clients for instance role: " + instanceRoleName, ex);
    }
    return results;
  }

  @Override
  public Optional<InstanceRoleRecord> findRoleForClientInWorld(String username, String worldName) {
    try (PreparedStatement ps = connection().prepareStatement(FIND_FOR_CLIENT_IN_WORLD_QUERY)) {
      ps.setString(1, username);
      ps.setString(2, worldName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapWithWorld(rs));
        }
      }
    } catch (SQLException ex) {
      throw new SqlCrudException(
          String.format("Failed to find role for client '%s' in world '%s'", username, worldName),
          ex);
    }
    return Optional.empty();
  }

  @Override
  public void assignRole(String username, String worldName, String instanceRoleName) {
    try (PreparedStatement ps = connection().prepareStatement(ASSIGN_ROLE_QUERY)) {
      ps.setString(1, username);
      ps.setString(2, worldName);
      ps.setString(3, instanceRoleName);
      int rows = ps.executeUpdate();
      if (rows == 0) {
        log.warn("[ClientsInstanceRoles] assignRole — 0 rows affected. "
            + "Check that username '{}' and role '{}' exist.", username, instanceRoleName);
      } else {
        log.debug("[ClientsInstanceRoles] Assigned role '{}' to '{}' in world '{}'.",
            instanceRoleName, username, worldName);
      }
    } catch (SQLException ex) {
      throw new SqlCrudException(
          String.format("Failed to assign role '%s' to '%s' in world '%s'",
              instanceRoleName, username, worldName), ex);
    }
  }

  @Override
  public void removeRole(String username, String worldName) {
    try (PreparedStatement ps = connection().prepareStatement(REMOVE_ROLE_QUERY)) {
      ps.setString(1, username);
      ps.setString(2, worldName);
      ps.executeUpdate();
      log.debug("[ClientsInstanceRoles] Removed role for '{}' in world '{}'.", username, worldName);
    } catch (SQLException ex) {
      throw new SqlCrudException(
          String.format("Failed to remove role for '%s' in world '%s'", username, worldName), ex);
    }
  }

  // ── Mapping ─────────────────────────────────────────────────────────────────

  private InstanceRoleRecord mapWithWorld(ResultSet rs) throws SQLException {
    return new InstanceRoleRecord(
        rs.getLong("instance_role_id"),
        rs.getString("instance_role_name"),
        rs.getString("world_name"));
  }
}
