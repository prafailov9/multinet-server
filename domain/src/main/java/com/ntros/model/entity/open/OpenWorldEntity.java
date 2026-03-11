package com.ntros.model.entity.open;

import com.ntros.model.entity.movement.Vector3D;
import com.ntros.model.entity.movement.Velocity3D;

/**
 * A freely-moving entity that lives in a continuous 3D open world.
 *
 * <p>Unlike {@link com.ntros.model.entity.Entity}, whose position is a discrete integer
 * {@link com.ntros.model.entity.movement.Position}, an {@code OpenWorldEntity} occupies a
 * floating-point coordinate in 3-dimensional space. Movement is physics-driven: callers push
 * thrust intents, the engine accumulates them into velocity, and
 * {@link #updatePosition(float)} integrates position each tick.
 */
public interface OpenWorldEntity {

  /** Human-readable name, also used as the primary key in world state maps. */
  String getName();

  /** Stable numeric id assigned at spawn time. */
  long getId();

  // ── Position / velocity ───────────────────────────────────────────────────

  Vector3D getPosition();

  void setPosition(Vector3D position);

  Velocity3D getVelocity();

  void setVelocity(Velocity3D velocity);

  /**
   * Integrates velocity into position over {@code deltaTime} seconds.
   *
   * @param deltaTime seconds since the last update (typically the server tick interval)
   */
  void updatePosition(float deltaTime);

  // ── Orientation ───────────────────────────────────────────────────────────

  /** Yaw angle in radians — rotation around the Y (vertical) axis. */
  float yaw();

  /** Pitch angle in radians — rotation around the X (lateral) axis (up/down tilt). */
  float pitch();

  // ── Movement parameters ───────────────────────────────────────────────────

  /** Acceleration magnitude (units/s²) applied when a thrust intent is processed. */
  float acceleration();

  /** Maximum allowed speed (units/s) — velocity is clamped to this value each tick. */
  float maxSpeed();
}
