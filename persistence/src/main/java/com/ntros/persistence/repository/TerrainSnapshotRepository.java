package com.ntros.persistence.repository;

import com.ntros.model.entity.movement.cell.Position;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.protocol.TileType;
import java.util.Map;
import java.util.Optional;

/**
 * Saves and loads the terrain of a grid world to/from durable storage so that the terrain is
 * stable across server restarts.
 *
 * <p>Without this, {@link com.ntros.model.world.state.solid.GridWorldState} re-generates terrain
 * randomly on every startup, making it impossible for players to memorise the map layout.
 */
public interface TerrainSnapshotRepository {

  /**
   * Persists the terrain for {@code worldName}, replacing any previously saved snapshot.
   *
   * @param worldName unique world name (used as the storage key / filename stem)
   * @param terrain   full terrain map — must not be null or empty
   */
  void save(String worldName, Map<Vector4, TileType> terrain);

  /**
   * Loads a previously saved terrain snapshot.
   *
   * @param worldName world name to load
   * @return the terrain map, or {@link Optional#empty()} if no snapshot exists yet
   */
  Optional<Map<Vector4, TileType>> load(String worldName);

  /**
   * Returns {@code true} if a snapshot file exists for {@code worldName}.
   */
  boolean exists(String worldName);

  /**
   * Deletes the terrain snapshot for {@code worldName}. No-op if no snapshot exists.
   *
   * <p>Used by the shutdown hook when a world is decommissioned, and in tests to reset
   * file state between scenarios.
   */
  void delete(String worldName);
}
