package com.ntros.persistence.repository.impl;

import com.ntros.persistence.db.ConnectionProvider;
import com.ntros.persistence.model.WorldRecord;
import com.ntros.persistence.repository.WorldRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * SQLite-backed implementation of {@link WorldRepository}.
 */
@Slf4j
public class SqliteWorldRepository implements WorldRepository {

  private static final String COLUMNS =
      "name, engine_type, width, height, depth, multiplayer, orchestrated, has_ai, deterministic, created_at";

  @Override
  public WorldRecord registerIfAbsent(WorldRecord record) {
    Optional<WorldRecord> existing = findByName(record.name());
    if (existing.isPresent()) {
      return existing.get();
    }

    try (PreparedStatement ps = conn().prepareStatement(
        "INSERT INTO worlds (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
      ps.setString(1, record.name());
      ps.setString(2, record.engineType());
      ps.setInt(3, record.width());
      ps.setInt(4, record.height());
      ps.setInt(5, record.depth());
      ps.setInt(6, record.multiplayer() ? 1 : 0);
      ps.setInt(7, record.orchestrated() ? 1 : 0);
      ps.setInt(8, record.hasAi() ? 1 : 0);
      ps.setInt(9, record.deterministic() ? 1 : 0);
      ps.setString(10, record.createdAt().toString());
      ps.executeUpdate();
      log.info("[WorldRepository] Registered world '{}'.", record.name());
    } catch (SQLException e) {
      throw new RuntimeException("Failed to register world: " + record.name(), e);
    }

    return findByName(record.name()).orElseThrow();
  }

  @Override
  public Optional<WorldRecord> findByName(String worldName) {
    try (PreparedStatement ps = conn().prepareStatement(
        "SELECT " + COLUMNS + " FROM worlds WHERE name = ?")) {
      ps.setString(1, worldName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(map(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find world: " + worldName, e);
    }
    return Optional.empty();
  }

  @Override
  public List<WorldRecord> findAll() {
    List<WorldRecord> result = new ArrayList<>();
    try (PreparedStatement ps = conn().prepareStatement(
        "SELECT " + COLUMNS + " FROM worlds ORDER BY created_at")) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(map(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to list worlds", e);
    }
    return result;
  }


  // ── Helper ────────────────────────────────────────────────────────────────

  private static WorldRecord map(ResultSet rs) throws SQLException {
    return new WorldRecord(
        rs.getString("name"),
        rs.getString("engine_type"),
        rs.getInt("width"),
        rs.getInt("height"),
        rs.getInt("depth"),
        rs.getInt("multiplayer") == 1,
        rs.getInt("orchestrated") == 1,
        rs.getInt("has_ai") == 1,
        rs.getInt("deterministic") == 1,
        Instant.parse(rs.getString("created_at"))
    );
  }

  private static Connection conn() {
    return ConnectionProvider.connection();
  }
}
