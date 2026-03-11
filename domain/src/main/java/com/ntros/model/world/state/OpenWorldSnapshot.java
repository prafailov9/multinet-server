package com.ntros.model.world.state;

import java.util.Map;

/**
 * Immutable snapshot of an open 3D world, suitable for JSON serialisation and broadcast.
 *
 * <p>Analogous to {@link GridSnapshot} for grid worlds; built by
 * {@link com.ntros.model.world.connector.OpenWorldConnector#snapshot()} each tick.
 *
 * @param bounds   world bounding box — width × height × depth (integer grid units)
 * @param entities entity name → 3D view of each entity's current state
 */
public record OpenWorldSnapshot(
    BoundsView bounds,
    Map<String, EntityView3D> entities
) {

  /**
   * Bounding volume of the world.
   */
  public record BoundsView(int width, int height, int depth) {

  }

  /**
   * Per-entity snapshot: position, velocity, and orientation angles.
   * @param x     position X
   * @param y     position Y (vertical)
   * @param z     position Z
   * @param dx    velocity X component
   * @param dy    velocity Y component
   * @param dz    velocity Z component
   * @param yaw   heading angle in radians (rotation around Y axis)
   * @param pitch tilt angle in radians (rotation around X axis)
   */
  public record EntityView3D(
      float x, float y, float z,
      float dx, float dy, float dz,
      float yaw, float pitch
  ) {

  }
}
