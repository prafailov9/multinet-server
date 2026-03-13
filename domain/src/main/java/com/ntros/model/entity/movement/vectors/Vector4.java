package com.ntros.model.entity.movement.vectors;


import com.ntros.model.entity.movement.cell.Position;
import java.util.Objects;
import lombok.Data;

/**
 * Immutable 4-dimensional float vector.
 * Used for multi-dimensional movement and higher-dimensional world simulations.
 */
public final class Vector4 {

  public static final Vector4 ZERO = new Vector4(0f, 0f, 0f, 0f);
  public static final Vector4 UP = new Vector4(0f, 1f, 0f, 0f);

  private final float x;
  private final float y;
  private final float z;
  private final float w;

  private Vector4(float x, float y, float z, float w) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.w = w;
  }

  public static Vector4 of(float x, float y, float z, float w) {
    return new Vector4(x, y, z, w);
  }

  public static Vector4 of2dGridPosition(Position position) {
    return new Vector4(position.getX(), position.getY(), 0f, 0f);
  }


  // Getters
  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  public float getZ() {
    return z;
  }

  public float getW() {
    return w;
  }

  // Arithmetic
  public Vector4 add(Vector4 other) {
    return new Vector4(
        x + other.x,
        y + other.y,
        z + other.z,
        w + other.w
    );
  }

  public Vector4 subtract(Vector4 other) {
    return new Vector4(
        x - other.x,
        y - other.y,
        z - other.z,
        w - other.w
    );
  }

  public Vector4 scale(float factor) {
    return new Vector4(
        x * factor,
        y * factor,
        z * factor,
        w * factor
    );
  }

  public Vector4 negate() {
    return new Vector4(-x, -y, -z, -w);
  }

  // magnitude / normalize

  public float magnitudeSquared() {
    return x * x + y * y + z * z + w * w;
  }

  public float magnitude() {
    return (float) Math.sqrt(magnitudeSquared());
  }

  /**
   * Returns a unit vector in the same direction, or {@link #ZERO} if this vector has zero length.
   */
  public Vector4 normalize() {
    float mag = magnitude();
    if (mag == 0f) {
      return ZERO;
    }
    return new Vector4(
        x / mag,
        y / mag,
        z / mag,
        w / mag
    );
  }

  /**
   * Clamps the magnitude to {@code maxLength}.
   */
  public Vector4 clampMagnitude(float maxLength) {
    if (magnitudeSquared() > maxLength * maxLength) {
      return normalize().scale(maxLength);
    }
    return this;
  }

  // Products

  public float dot(Vector4 other) {
    return x * other.x
        + y * other.y
        + z * other.z
        + w * other.w;
  }

  // Distance

  public float distance(Vector4 other) {
    return subtract(other).magnitude();
  }

  public float distanceSquared(Vector4 other) {
    float dx = other.x - x;
    float dy = other.y - y;
    float dz = other.z - z;
    float dw = other.w - w;

    return dx * dx + dy * dy + dz * dz + dw * dw;
  }

  // Interpolation

  public Vector4 lerp(Vector4 target, float t) {
    return new Vector4(
        x + (target.x - x) * t,
        y + (target.y - y) * t,
        z + (target.z - z) * t,
        w + (target.w - w) * t
    );
  }


  /**
   * JMC profiler shows that using Lombok's @Data generates hashCode() as Objects.hash(x, y, z, w)
   * — and Objects.hash takes a Object... vararg, which autoboxes every float to a Float.
   * Four boxes per key operation. Every terrain.put, terrain.remove, and terrain.getOrDefault in
   * steps 1 and 4 pays this. The profiler shows Float.valueOf(float) ← Vector4.hashCode() ←
   * HashMap.hash()
   * in both the tick path and the RANDOM_SEED path. This single bug is responsible
   * for the 2719 Float allocation samples.
   * The fix is to stop boxing in hashCode(). Instead use Float.floatToIntBits() which takes a
   * primitive, no boxing.
   *
   *
   */
  @Override
  public int hashCode() {
    int result = Float.floatToIntBits(x);
    result = 31 * result + Float.floatToIntBits(y);
    result = 31 * result + Float.floatToIntBits(z);
    result = 31 * result + Float.floatToIntBits(w);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Vector4 v)) {
      return false;
    }
    return Float.compare(x, v.x) == 0
        && Float.compare(y, v.y) == 0
        && Float.compare(z, v.z) == 0
        && Float.compare(w, v.w) == 0;
  }

  @Override
  public String toString() {
    return "Vector4{x=" + x +
        ", y=" + y +
        ", z=" + z +
        ", w=" + w +
        '}';
  }

}