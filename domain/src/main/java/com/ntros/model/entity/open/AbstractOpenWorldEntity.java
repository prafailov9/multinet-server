package com.ntros.model.entity.open;

import com.ntros.model.entity.movement.Vector3D;
import com.ntros.model.entity.movement.Velocity3D;

/**
 * Base implementation of {@link OpenWorldEntity} that handles position integration and provides
 * orientation derived from the current velocity vector.
 */
public abstract class AbstractOpenWorldEntity implements OpenWorldEntity {

  protected volatile Vector3D  position;
  protected volatile Velocity3D velocity;

  protected AbstractOpenWorldEntity(Vector3D position, Velocity3D velocity) {
    this.position = position;
    this.velocity = velocity;
  }

  // ── Position / velocity ───────────────────────────────────────────────────

  @Override
  public Vector3D getPosition() {
    return position;
  }

  @Override
  public void setPosition(Vector3D position) {
    this.position = position;
  }

  @Override
  public Velocity3D getVelocity() {
    return velocity;
  }

  @Override
  public void setVelocity(Velocity3D velocity) {
    this.velocity = velocity;
  }

  /**
   * Euler integration: pos += vel * dt.
   * Called by the engine on the actor thread; {@code volatile} writes guarantee visibility.
   */
  @Override
  public void updatePosition(float deltaTime) {
    position = Vector3D.of(
        position.getX() + velocity.getDx() * deltaTime,
        position.getY() + velocity.getDy() * deltaTime,
        position.getZ() + velocity.getDz() * deltaTime
    );
  }

  // ── Orientation derived from velocity ─────────────────────────────────────

  /**
   * Yaw is the angle in the XZ plane (horizontal heading), measured in radians.
   * Returns 0 when the entity is stationary.
   */
  @Override
  public float yaw() {
    return (float) Math.atan2(velocity.getDx(), velocity.getDz());
  }

  /**
   * Pitch is the vertical tilt angle, measured in radians.
   * Positive pitch = nose up; negative pitch = nose down.
   */
  @Override
  public float pitch() {
    float horizontalSpeed = (float) Math.sqrt(
        velocity.getDx() * velocity.getDx() + velocity.getDz() * velocity.getDz()
    );
    return (float) Math.atan2(velocity.getDy(), horizontalSpeed);
  }
}
