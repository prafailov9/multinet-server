package com.ntros.model.entity.movement;

import java.util.Objects;

public class Vector2D implements Vector {

  public static final Vector2D ZERO = new Vector2D(0, 0);

  private final float x;
  private final float y;

  private Vector2D(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public static Vector2D of(float x, float y) {
    return new Vector2D(x, y);
  }

  @Override
  public float getX() {
    return x;
  }

  @Override
  public float getY() {
    return y;
  }

  // Basic arithmetic

  public Vector2D add(Vector2D other) {
    return new Vector2D(x + other.x, y + other.y);
  }

  public Vector2D subtract(Vector2D other) {
    return new Vector2D(x - other.x, y - other.y);
  }

  public Vector2D scale(float factor) {
    return new Vector2D(x * factor, y * factor);
  }

  // Magnitude

  public float magnitude() {
    return (float) Math.sqrt(x * x + y * y);
  }

  public float magnitudeSquared() {
    return x * x + y * y;
  }

  // Normalize

  public Vector2D normalize() {
    float mag = magnitude();
    if (mag == 0) {
      return ZERO;
    }
    return new Vector2D(x / mag, y / mag);
  }

  // Dot product

  public float dot(Vector2D other) {
    return x * other.x + y * other.y;
  }

  // Distance

  public float distance(Vector2D other) {
    return subtract(other).magnitude();
  }

  public float distanceSquared(Vector2D other) {
    float dx = other.x - x;
    float dy = other.y - y;
    return dx * dx + dy * dy;
  }

  // Angle (radians)

  public float angle() {
    return (float) Math.atan2(y, x);
  }

  // Rotate vector

  public Vector2D rotate(float radians) {
    float cos = (float) Math.cos(radians);
    float sin = (float) Math.sin(radians);

    return new Vector2D(x * cos - y * sin, x * sin + y * cos);
  }

  // Clamp magnitude (limit speed)

  public Vector2D clampMagnitude(float maxLength) {
    float magSq = magnitudeSquared();

    if (magSq > maxLength * maxLength) {
      return normalize().scale(maxLength);
    }

    return this;
  }

  // Linear interpolation

  public Vector2D lerp(Vector2D target, float t) {
    return new Vector2D(x + (target.x - x) * t, y + (target.y - y) * t);
  }

  public static Vector2D randomUnit() {
    double angle = Math.random() * Math.PI * 2;
    return new Vector2D(
        (float) Math.cos(angle),
        (float) Math.sin(angle)
    );
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Vector2D v)) {
      return false;
    }
    return Float.compare(x, v.x) == 0 && Float.compare(y, v.y) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }

  @Override
  public String toString() {
    return "Vector2D{x=" + x + ", y=" + y + '}';
  }
}
