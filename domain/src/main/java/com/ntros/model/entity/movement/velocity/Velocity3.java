package com.ntros.model.entity.movement.velocity;

import com.ntros.model.entity.movement.vectors.Vector3;
import java.util.Objects;

/**
 * Immutable 3-dimensional velocity. Stores the per-axis delta (dx, dy, dz) applied each second.
 */
public final class Velocity3 {

  public static final Velocity3 ZERO = new Velocity3(0f, 0f, 0f);

  private final float dx;
  private final float dy;
  private final float dz;

  private Velocity3(float dx, float dy, float dz) {
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
  }

  public static Velocity3 of(float dx, float dy, float dz) {
    return new Velocity3(dx, dy, dz);
  }

  // ── Accessors ─────────────────────────────────────────────────────────────

  public float getDx() { return dx; }
  public float getDy() { return dy; }
  public float getDz() { return dz; }

  // ── Arithmetic ────────────────────────────────────────────────────────────

  /** Adds a direction vector (acceleration impulse) and returns a new velocity. */
  public Velocity3 add(Vector3 impulse) {
    return new Velocity3(dx + impulse.getX(), dy + impulse.getY(), dz + impulse.getZ());
  }

  public Velocity3 scale(float factor) {
    return new Velocity3(dx * factor, dy * factor, dz * factor);
  }

  // ── Speed ─────────────────────────────────────────────────────────────────

  public float speedSquared() {
    return dx * dx + dy * dy + dz * dz;
  }

  public float speed() {
    return (float) Math.sqrt(speedSquared());
  }

  /**
   * Returns a new velocity clamped so that the overall speed does not exceed {@code maxSpeed}.
   * The direction is preserved.
   */
  public Velocity3 clampSpeed(float maxSpeed) {
    float s = speed();
    if (s <= maxSpeed || s == 0f) {
      return this;
    }
    float scale = maxSpeed / s;
    return new Velocity3(dx * scale, dy * scale, dz * scale);
  }

  /** Converts this velocity to an equivalent direction vector. */
  public Vector3 asVector() {
    return Vector3.of(dx, dy, dz);
  }

  // ── Object ────────────────────────────────────────────────────────────────

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Velocity3 that)) {
      return false;
    }
    return Float.compare(dx, that.dx) == 0
        && Float.compare(dy, that.dy) == 0
        && Float.compare(dz, that.dz) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dx, dy, dz);
  }

  @Override
  public String toString() {
    return "Velocity3D{dx=" + dx + ", dy=" + dy + ", dz=" + dz + '}';
  }
}
