package com.ntros.persistence.repository.impl;

import static com.ntros.persistence.db.ConnectionProvider.connection;

import com.ntros.persistence.exception.SqlCrudException;
import com.ntros.persistence.model.ClientRecord;
import com.ntros.persistence.model.InstanceRoleRecord;
import com.ntros.persistence.model.SystemRoleRecord;
import com.ntros.persistence.repository.ClientRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * SQLite-backed implementation of {@link ClientRepository}.
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>The {@code clients} table has no {@code client_role} column. System role is stored via FK
 *   {@code system_role_id → system_roles}; world-scoped (instance) roles live in
 *   {@code clients_instance_roles}.</li>
 *
 *   <li>{@link #findByUsername} and siblings do a {@code LEFT JOIN} on {@code system_roles} so
 *   the {@link SystemRoleRecord} is always populated in a single round-trip.</li>
 *
 *   <li>Instance roles are intentionally <em>not</em> loaded here — they are world-scoped and
 *   should be fetched on-demand via {@link SqliteClientsInstanceRolesRepository} (e.g., during
 *   JOIN command processing). This keeps the common "auth check" path cheap.</li>
 *
 *   <li>INSERT embeds a sub-select for {@code system_role_id} by name, so callers supply the
 *   role name string and never manage integer FK values directly.</li>
 * </ul>
 */
@Slf4j
public class SqliteClientRepository implements ClientRepository {

  // ── Queries ─────────────────────────────────────────────────────────────────

  private static final String FIND_BY_USERNAME_QUERY = """
      SELECT c.client_id, c.session_id, c.username, c.password, c.created_at, c.updated_at,
             sr.system_role_id, sr.system_role_name
      FROM clients c
      LEFT JOIN system_roles sr ON c.system_role_id = sr.system_role_id
      WHERE c.username = ?
      """;

  private static final String FIND_BY_ID_QUERY = """
      SELECT c.client_id, c.session_id, c.username, c.password, c.created_at, c.updated_at,
             sr.system_role_id, sr.system_role_name
      FROM clients c
      LEFT JOIN system_roles sr ON c.system_role_id = sr.system_role_id
      WHERE c.client_id = ?
      """;

  private static final String FIND_ALL_QUERY = """
      SELECT c.client_id, c.session_id, c.username, c.password, c.created_at, c.updated_at,
             sr.system_role_id, sr.system_role_name
      FROM clients c
      LEFT JOIN system_roles sr ON c.system_role_id = sr.system_role_id
      ORDER BY c.created_at
      """;

  /**
   * Params: (session_id, username, password, system_role_name, created_at, updated_at)
   * {@code system_role_id} is resolved by name through a sub-select.
   */
  private static final String INSERT_QUERY = """
      INSERT INTO clients (session_id, username, password, system_role_id, created_at, updated_at)
      VALUES (?, ?, ?, (SELECT system_role_id FROM system_roles WHERE system_role_name = ?), ?, ?)
      """;

  /**
   * Params: (session_id, username, password, system_role_name, created_at, updated_at, client_id)
   */
  private static final String UPDATE_QUERY = """
      UPDATE clients
      SET session_id     = ?,
          username       = ?,
          password       = ?,
          system_role_id = (SELECT system_role_id FROM system_roles WHERE system_role_name = ?),
          created_at     = ?,
          updated_at     = ?
      WHERE client_id    = ?
      """;

  /** Params: (system_role_name, username) */
  private static final String UPDATE_SYSTEM_ROLE_QUERY = """
      UPDATE clients
      SET system_role_id = (SELECT system_role_id FROM system_roles WHERE system_role_name = ?)
      WHERE username = ?
      """;

  // ── ClientRepository ────────────────────────────────────────────────────────

  @Override
  public Optional<ClientRecord> findByUsername(String username) {
    try (PreparedStatement ps = connection().prepareStatement(FIND_BY_USERNAME_QUERY)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(map(rs));
        }
      }
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to find client by username: " + username, ex);
    }
    return Optional.empty();
  }

  @Override
  public Optional<ClientRecord> findById(long clientId) {
    try (PreparedStatement ps = connection().prepareStatement(FIND_BY_ID_QUERY)) {
      ps.setLong(1, clientId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(map(rs));
        }
      }
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to find client by id: " + clientId, ex);
    }
    return Optional.empty();
  }

  @Override
  public List<ClientRecord> findAll() {
    List<ClientRecord> results = new ArrayList<>();
    try (PreparedStatement ps = connection().prepareStatement(FIND_ALL_QUERY);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        results.add(map(rs));
      }
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to list all clients", ex);
    }
    return results;
  }

  @Override
  public Optional<ClientRecord> insert(ClientRecord client) {
    if (findByUsername(client.username()).isPresent()) {
      log.debug("[ClientRepository] insert skipped — '{}' already exists.", client.username());
      return findByUsername(client.username());
    }
    try (PreparedStatement ps = connection().prepareStatement(INSERT_QUERY)) {
      bindInsertParams(ps, client);
      ps.executeUpdate();
      log.info("[ClientRepository] Inserted client '{}'.", client.username());
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to insert client: " + client.username(), ex);
    }
    return findByUsername(client.username());
  }

  @Override
  public Optional<ClientRecord> upsert(ClientRecord client) {
    Optional<ClientRecord> existing = findByUsername(client.username());
    if (existing.isPresent()) {
      try (PreparedStatement ps = connection().prepareStatement(UPDATE_QUERY)) {
        bindUpdateParams(ps, client);
        ps.executeUpdate();
        log.debug("[ClientRepository] Updated client '{}'.", client.username());
      } catch (SQLException ex) {
        throw new SqlCrudException("Failed to update client: " + client.username(), ex);
      }
    } else {
      try (PreparedStatement ps = connection().prepareStatement(INSERT_QUERY)) {
        bindInsertParams(ps, client);
        ps.executeUpdate();
        log.info("[ClientRepository] Upsert — inserted new client '{}'.", client.username());
      } catch (SQLException ex) {
        throw new SqlCrudException("Failed to upsert (insert) client: " + client.username(), ex);
      }
    }
    return findByUsername(client.username());
  }

  @Override
  public ClientRecord registerIfAbsent(ClientRecord client) {
    return findByUsername(client.username()).orElseGet(() -> {
      try (PreparedStatement ps = connection().prepareStatement(INSERT_QUERY)) {
        bindInsertParams(ps, client);
        ps.executeUpdate();
        log.info("[ClientRepository] Registered client '{}'.", client.username());
      } catch (SQLException ex) {
        throw new SqlCrudException("Failed to register client: " + client.username(), ex);
      }
      return findByUsername(client.username()).orElseThrow(() ->
          new SqlCrudException("Client not found after insert: " + client.username()));
    });
  }

  /**
   * Updates only the <em>system</em> role for a client (i.e., the server-wide privilege level).
   * For world-scoped (instance) role changes use
   * {@link SqliteClientsInstanceRolesRepository#assignRole}.
   *
   * @param username       the client's username
   * @param systemRoleName name of the new system role — must match a row in {@code system_roles}
   */
  @Override
  public void updateRole(String username, String systemRoleName) {
    try (PreparedStatement ps = connection().prepareStatement(UPDATE_SYSTEM_ROLE_QUERY)) {
      ps.setString(1, systemRoleName);
      ps.setString(2, username);
      int rows = ps.executeUpdate();
      if (rows == 0) {
        log.warn("[ClientRepository] updateRole — no rows updated for username '{}'.", username);
      }
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to update system role for client: " + username, ex);
    }
  }

  // TODO: implement account deletion when the feature is needed
  @Override
  public Optional<ClientRecord> remove(ClientRecord client) {
    return Optional.empty();
  }

  @Override
  public Optional<ClientRecord> removeById(long clientId) {
    return Optional.empty();
  }

  // ── Mapping ─────────────────────────────────────────────────────────────────

  private ClientRecord map(ResultSet rs) throws SQLException {
    long clientId  = rs.getLong("client_id");
    long sessionId = rs.getLong("session_id");
    String username  = rs.getString("username");
    String password  = rs.getString("password");
    Instant createdAt = parseInstant(rs.getString("created_at"));
    Instant updatedAt = parseInstant(rs.getString("updated_at"));

    SystemRoleRecord systemRole = new SystemRoleRecord(
        rs.getLong("system_role_id"),
        rs.getString("system_role_name"));

    // Instance roles are not loaded in bulk — see class-level Javadoc.
    List<InstanceRoleRecord> instanceRoles = List.of();

    return new ClientRecord(clientId, sessionId, systemRole, instanceRoles,
        username, password, createdAt, updatedAt);
  }

  // ── Parameter binding ────────────────────────────────────────────────────────

  /** Params: (session_id, username, password, system_role_name, created_at, updated_at) */
  private void bindInsertParams(PreparedStatement ps, ClientRecord client) throws SQLException {
    ps.setLong(1, client.sessionId());
    ps.setString(2, client.username());
    ps.setString(3, client.password());
    ps.setString(4, resolveSystemRoleName(client));
    ps.setString(5, instantStr(client.createdAt()));
    ps.setString(6, instantStr(client.updatedAt()));
  }

  /** Params: (session_id, username, password, system_role_name, created_at, updated_at, client_id) */
  private void bindUpdateParams(PreparedStatement ps, ClientRecord client) throws SQLException {
    ps.setLong(1, client.sessionId());
    ps.setString(2, client.username());
    ps.setString(3, client.password());
    ps.setString(4, resolveSystemRoleName(client));
    ps.setString(5, instantStr(client.createdAt()));
    ps.setString(6, instantStr(client.updatedAt()));
    ps.setLong(7, client.clientId());
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private String resolveSystemRoleName(ClientRecord client) {
    SystemRoleRecord sr = client.systemRoleRecord();
    if (sr == null || sr.getSystemRoleName() == null || sr.getSystemRoleName().isBlank()) {
      return "USER"; // safe default — matches the USER row seeded by SchemaInitializer
    }
    return sr.getSystemRoleName();
  }

  private String instantStr(Instant instant) {
    return instant != null ? instant.toString() : Instant.now().toString();
  }

  private Instant parseInstant(String str) {
    return str != null ? Instant.parse(str) : null;
  }
}
