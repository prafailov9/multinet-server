package com.ntros.model.entity.movement;

import java.util.Objects;

/**
 * Immutable 3-dimensional float vector. Used for open-world positions and directions.
 */
public final class Vector3D {

  public static final Vector3D ZERO = new Vector3D(0f, 0f, 0f);
  public static final Vector3D UP   = new Vector3D(0f, 1f, 0f);

  private final float x;
  private final float y;
  private final float z;

  private Vector3D(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public static Vector3D of(float x, float y, float z) {
    return new Vector3D(x, y, z);
  }

  // ── Accessors ─────────────────────────────────────────────────────────────

  public float getX() { return x; }
  public float getY() { return y; }
  public float getZ() { return z; }

  // ── Arithmetic ────────────────────────────────────────────────────────────

  public Vector3D add(Vector3D other) {
    return new Vector3D(x + other.x, y + other.y, z + other.z);
  }

  public Vector3D subtract(Vector3D other) {
    return new Vector3D(x - other.x, y - other.y, z - other.z);
  }

  public Vector3D scale(float factor) {
    return new Vector3D(x * factor, y * factor, z * factor);
  }

  public Vector3D negate() {
    return new Vector3D(-x, -y, -z);
  }

  // ── Magnitude / normalise ─────────────────────────────────────────────────

  public float magnitudeSquared() {
    return x * x + y * y + z * z;
  }

  public float magnitude() {
    return (float) Math.sqrt(magnitudeSquared());
  }

  /**
   * Returns a unit vector in the same direction, or {@link #ZERO} if this vector has zero length.
   */
  public Vector3D normalize() {
    float mag = magnitude();
    if (mag == 0f) {
      return ZERO;
    }
    return new Vector3D(x / mag, y / mag, z / mag);
  }

  /**
   * Clamps the magnitude to {@code maxLength}. Returns {@code this} unchanged if already within
   * the limit.
   */
  public Vector3D clampMagnitude(float maxLength) {
    if (magnitudeSquared() > maxLength * maxLength) {
      return normalize().scale(maxLength);
    }
    return this;
  }

  // ── Products ──────────────────────────────────────────────────────────────

  public float dot(Vector3D other) {
    return x * other.x + y * other.y + z * other.z;
  }

  public Vector3D cross(Vector3D other) {
    return new Vector3D(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    );
  }

  // ── Distance ──────────────────────────────────────────────────────────────

  public float distance(Vector3D other) {
    return subtract(other).magnitude();
  }

  public float distanceSquared(Vector3D other) {
    float dx = other.x - x;
    float dy = other.y - y;
    float dz = other.z - z;
    return dx * dx + dy * dy + dz * dz;
  }

  // ── Interpolation ─────────────────────────────────────────────────────────

  public Vector3D lerp(Vector3D target, float t) {
    return new Vector3D(
        x + (target.x - x) * t,
        y + (target.y - y) * t,
        z + (target.z - z) * t
    );
  }

  // ── Factory helpers ───────────────────────────────────────────────────────

  public static Vector3D randomUnit() {
    // Uniform distribution on the sphere via spherical coordinates
    double theta = Math.random() * Math.PI * 2;      // azimuth [0, 2π)
    double phi   = Math.acos(1 - 2 * Math.random());  // polar   [0, π]
    return new Vector3D(
        (float) (Math.sin(phi) * Math.cos(theta)),
        (float) (Math.sin(phi) * Math.sin(theta)),
        (float) Math.cos(phi)
    );
  }

  // ── Object ────────────────────────────────────────────────────────────────

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Vector3D v)) {
      return false;
    }
    return Float.compare(x, v.x) == 0
        && Float.compare(y, v.y) == 0
        && Float.compare(z, v.z) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y, z);
  }

  @Override
  public String toString() {
    return "Vector3D{x=" + x + ", y=" + y + ", z=" + z + '}';
  }
}
