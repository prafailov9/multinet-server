package com.ntros.persistence.repository.impl;

import static com.ntros.persistence.db.ConnectionProvider.connection;

import com.ntros.persistence.exception.SqlCrudException;
import com.ntros.persistence.model.InstanceRoleRecord;
import com.ntros.persistence.repository.InstanceRoleRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqliteInstanceRoleRepository implements InstanceRoleRepository {

  private static final String FIND_ALL_QUERY =
      "SELECT instance_role_id, instance_role_name FROM instance_roles ORDER BY instance_role_id";

  private static final String FIND_BY_NAME_QUERY =
      "SELECT instance_role_id, instance_role_name FROM instance_roles WHERE instance_role_name = ?";

  @Override
  public List<InstanceRoleRecord> findAll() {
    List<InstanceRoleRecord> results = new ArrayList<>();
    try (PreparedStatement ps = connection().prepareStatement(FIND_ALL_QUERY);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        // Reference-data only — no worldName context at this level
        results.add(new InstanceRoleRecord(rs.getLong("instance_role_id"),
            rs.getString("instance_role_name"), null));
      }
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to list instance roles", ex);
    }
    return results;
  }

  @Override
  public Optional<InstanceRoleRecord> findByName(String instanceRoleName) {
    try (PreparedStatement ps = connection().prepareStatement(FIND_BY_NAME_QUERY)) {
      ps.setString(1, instanceRoleName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(new InstanceRoleRecord(rs.getLong("instance_role_id"),
              rs.getString("instance_role_name"), null));
        }
      }
    } catch (SQLException ex) {
      throw new SqlCrudException("Failed to find instance role by name: " + instanceRoleName, ex);
    }
    return Optional.empty();
  }
}
