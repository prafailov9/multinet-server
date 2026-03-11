package com.ntros.persistence.repository.impl;

import com.ntros.persistence.db.ConnectionProvider;
import com.ntros.persistence.model.PlayerRecord;
import com.ntros.persistence.repository.PlayerRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * SQLite-backed implementation of {@link PlayerRepository}.
 *
 * <p>All operations share the single JDBC connection managed by {@link ConnectionProvider}.
 * Because SQLite serialises writes and all persistence calls happen on a single thread
 * (JVM shutdown hook or session lifecycle callback), no additional synchronisation is needed.
 */
@Slf4j
public class SqlitePlayerRepository implements PlayerRepository {

  @Override
  public PlayerRecord upsert(String playerName) {
    // Check if player exists first
    Optional<PlayerRecord> existing = findByName(playerName);
    if (existing.isPresent()) {
      // Update last_seen only
      try (PreparedStatement ps = conn().prepareStatement(
          "UPDATE players SET last_seen = ? WHERE name = ?")) {
        ps.setString(1, Instant.now().toString());
        ps.setString(2, playerName);
        ps.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException("Failed to update player: " + playerName, e);
      }
      return findByName(playerName).orElseThrow();
    }

    // Insert new player
    try (PreparedStatement ps = conn().prepareStatement(
        "INSERT INTO players (name, total_moves, total_sessions, last_seen) VALUES (?, 0, 0, ?)")) {
      ps.setString(1, playerName);
      ps.setString(2, Instant.now().toString());
      ps.executeUpdate();
      log.info("[PlayerRepository] New player persisted: '{}'.", playerName);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to insert player: " + playerName, e);
    }

    return findByName(playerName).orElseThrow();
  }

  @Override
  public Optional<PlayerRecord> findByName(String name) {
    try (PreparedStatement ps = conn().prepareStatement(
        "SELECT id, name, total_moves, total_sessions, last_seen FROM players WHERE name = ?")) {
      ps.setString(1, name);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(map(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find player: " + name, e);
    }
    return Optional.empty();
  }

  @Override
  public void incrementMoves(String name, int delta) {
    if (delta <= 0) {
      return;
    }
    try (PreparedStatement ps = conn().prepareStatement(
        "UPDATE players SET total_moves = total_moves + ? WHERE name = ?")) {
      ps.setInt(1, delta);
      ps.setString(2, name);
      int rows = ps.executeUpdate();
      if (rows == 0) {
        log.warn("[PlayerRepository] incrementMoves: player '{}' not found — skipped.", name);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to increment moves for: " + name, e);
    }
  }

  @Override
  public void recordSessionEnd(String name) {
    try (PreparedStatement ps = conn().prepareStatement(
        "UPDATE players SET total_sessions = total_sessions + 1, last_seen = ? WHERE name = ?")) {
      ps.setString(1, Instant.now().toString());
      ps.setString(2, name);
      int rows = ps.executeUpdate();
      if (rows == 0) {
        log.warn("[PlayerRepository] recordSessionEnd: player '{}' not found — skipped.", name);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to record session end for: " + name, e);
    }
  }

  private static PlayerRecord map(ResultSet rs) throws SQLException {
    String lastSeenStr = rs.getString("last_seen");
    Instant lastSeen = lastSeenStr != null ? Instant.parse(lastSeenStr) : null;
    return new PlayerRecord(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getInt("total_moves"),
        rs.getInt("total_sessions"),
        lastSeen
    );
  }

  private static Connection conn() {
    return ConnectionProvider.connection();
  }
}
