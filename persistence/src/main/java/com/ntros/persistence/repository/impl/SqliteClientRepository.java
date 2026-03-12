package com.ntros.persistence.repository.impl;

import static com.ntros.persistence.db.ConnectionProvider.connection;

import com.ntros.persistence.exception.SqlCrudException;
import com.ntros.persistence.model.ClientRecord;
import com.ntros.persistence.repository.ClientRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqliteClientRepository implements ClientRepository {

  private static final String FIND_BY_USERNAME_QUERY = "SELECT * FROM clients WHERE username = ?";
  private static final String FIND_BY_ID_QUERY = "SELECT * FROM clients WHERE client_id = ?";

  private static final String INSERT_CLIENT_QUERY = "INSERT INTO clients (session_id, username, password, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";


  private static final String COLUMNS = "client_id, session_id, username, password, created_at, updated_at";

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
      throw new SqlCrudException(String.format("Failed to find client with username: %s", username),
          ex);
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
    } catch (SQLException e) {
      throw new SqlCrudException(
          String.format("[ClientRepo]: could not find client with clientId: %s", clientId), e);
    }
    return Optional.empty();
  }

  @Override
  public Optional<ClientRecord> insert(ClientRecord client) {
    // check if exist
    Optional<ClientRecord> exist = findByUsername(client.username());
    if (exist.isPresent()) {
      return exist;
    }

    // not exist - insert new entity
    try (PreparedStatement ps = connection().prepareStatement(INSERT_CLIENT_QUERY)) {
      // set params (session_id, username, password, created_at, updated_at)
      setStatementParams(ps, client);
      ps.executeUpdate();
      log.info("[ClientRepository]: New client saved: '{}'.", client.username());
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to insert client: " + client.username(), ex);
    }
    return findByUsername(client.username());
  }

  // TODO: implement removes
  @Override
  public Optional<ClientRecord> remove(ClientRecord client) {
    return Optional.empty();
  }

  @Override
  public Optional<ClientRecord> removeById(long clientId) {
    return Optional.empty();
  }

  @Override
  public List<ClientRecord> findAll() {
    List<ClientRecord> result = new ArrayList<>();
    try (PreparedStatement ps = connection().prepareStatement(
        "SELECT " + COLUMNS + " FROM clients ORDER BY created_at")) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(map(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to list clients", e);
    }
    return result;
  }

  @Override
  public ClientRecord registerIfAbsent(ClientRecord client) {
    Optional<ClientRecord> existing = findByUsername(client.username());
    if (existing.isPresent()) {
      return existing.get();
    }

    try (PreparedStatement ps = connection().prepareStatement(
        "INSERT INTO clients (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)")) {
      setStatementParams(ps, client);

      ps.executeUpdate();
      log.info("[ClientRepository] Registered client '{}'.", client);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to register client: " + client.username(), e);
    }

    return findByUsername(client.username()).orElseThrow();
  }

  private static ClientRecord map(ResultSet rs) throws SQLException {
    String createdAtStr = rs.getString("created_at");
    String updatedAtStr = rs.getString("updated_at");

    return new ClientRecord(
        rs.getLong("client_id"),
        rs.getLong("session_id"),
        rs.getString("username"),
        rs.getString("password"),
        createdAtStr != null ? Instant.parse(createdAtStr) : null,
        updatedAtStr != null ? Instant.parse(updatedAtStr) : null
    );
  }

  private void setStatementParams(PreparedStatement ps, ClientRecord client) throws SQLException {
    ps.setLong(1, client.sessionId());
    ps.setString(2, client.username());
    ps.setString(3, client.password());
    ps.setString(4, client.createdAt().toString());
    ps.setString(5, client.updatedAt().toString());
  }

}
