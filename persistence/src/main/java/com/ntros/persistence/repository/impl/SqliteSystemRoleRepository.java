package com.ntros.persistence.repository.impl;

import static com.ntros.persistence.db.ConnectionProvider.connection;

import com.ntros.persistence.exception.SqlCrudException;
import com.ntros.persistence.model.SystemRoleRecord;
import com.ntros.persistence.repository.SystemRoleRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqliteSystemRoleRepository implements SystemRoleRepository {

  private static final String FIND_ALL_QUERY =
      "SELECT system_role_id, system_role_name FROM system_roles ORDER BY system_role_id";

  private static final String FIND_BY_NAME_QUERY =
      "SELECT system_role_id, system_role_name FROM system_roles WHERE system_role_name = ?";

  @Override
  public List<SystemRoleRecord> findAll() {
    List<SystemRoleRecord> results = new ArrayList<>();
    try (PreparedStatement ps = connection().prepareStatement(FIND_ALL_QUERY);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        results.add(map(rs));
      }
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to list system roles", ex);
    }
    return results;
  }

  @Override
  public Optional<SystemRoleRecord> findByName(String systemRoleName) {
    try (PreparedStatement ps = connection().prepareStatement(FIND_BY_NAME_QUERY)) {
      ps.setString(1, systemRoleName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(map(rs));
        }
      }
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to find system role by name: " + systemRoleName, ex);
    }
    return Optional.empty();
  }

  private SystemRoleRecord map(ResultSet rs) throws SQLException {
    return new SystemRoleRecord(rs.getLong("system_role_id"), rs.getString("system_role_name"));
  }
}
