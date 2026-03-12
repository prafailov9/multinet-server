package com.ntros.model.entity.movement.velocity;

import com.ntros.model.entity.movement.vectors.Vector4;
import java.util.Objects;
import lombok.Data;

/**
 * Immutable 4-dimensional velocity.
 * Stores the per-axis delta (dx, dy, dz, dw) applied each second.
 */
@Data
public final class Velocity4 {

  public static final Velocity4 ZERO = new Velocity4(0f, 0f, 0f, 0f);

  private final float dx;
  private final float dy;
  private final float dz;
  private final float dw;

  private Velocity4(float dx, float dy, float dz, float dw) {
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.dw = dw;
  }

  public static Velocity4 of(float dx, float dy, float dz, float dw) {
    return new Velocity4(dx, dy, dz, dw);
  }

  // ── Arithmetic ────────────────────────────────────────────────────────────

  /**
   * Adds a direction vector (acceleration impulse) and returns a new velocity.
   */
  public Velocity4 add(Vector4 impulse) {
    return new Velocity4(
        dx + impulse.getX(),
        dy + impulse.getY(),
        dz + impulse.getZ(),
        dw + impulse.getW()
    );
  }

  public Velocity4 scale(float factor) {
    return new Velocity4(
        dx * factor,
        dy * factor,
        dz * factor,
        dw * factor
    );
  }

  // ── Speed ─────────────────────────────────────────────────────────────────

  public float speedSquared() {
    return dx * dx + dy * dy + dz * dz + dw * dw;
  }

  public float speed() {
    return (float) Math.sqrt(speedSquared());
  }

  /**
   * Returns a new velocity clamped so that the overall speed does not exceed {@code maxSpeed}.
   * The direction is preserved.
   */
  public Velocity4 clampSpeed(float maxSpeed) {
    float s = speed();
    if (s <= maxSpeed || s == 0f) {
      return this;
    }

    float scale = maxSpeed / s;

    return new Velocity4(
        dx * scale,
        dy * scale,
        dz * scale,
        dw * scale
    );
  }

  /**
   * Converts this velocity to an equivalent direction vector.
   */
  public Vector4 asVector() {
    return Vector4.of(dx, dy, dz, dw);
  }

  // ── Object ────────────────────────────────────────────────────────────────

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Velocity4 that)) {
      return false;
    }

    return Float.compare(dx, that.dx) == 0
        && Float.compare(dy, that.dy) == 0
        && Float.compare(dz, that.dz) == 0
        && Float.compare(dw, that.dw) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dx, dy, dz, dw);
  }

  @Override
  public String toString() {
    return "Velocity4{" +
        "dx=" + dx +
        ", dy=" + dy +
        ", dz=" + dz +
        ", dw=" + dw +
        '}';
  }
}