package com.ntros.model.entity.movement.vectors;

import java.util.Objects;

public class Vector2 implements Vector {

  public static final Vector2 ZERO = new Vector2(0, 0);

  private final float x;
  private final float y;

  private Vector2(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public static Vector2 of(float x, float y) {
    return new Vector2(x, y);
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

  public Vector2 add(Vector2 other) {
    return new Vector2(x + other.x, y + other.y);
  }

  public Vector2 subtract(Vector2 other) {
    return new Vector2(x - other.x, y - other.y);
  }

  public Vector2 scale(float factor) {
    return new Vector2(x * factor, y * factor);
  }

  // Magnitude

  public float magnitude() {
    return (float) Math.sqrt(x * x + y * y);
  }

  public float magnitudeSquared() {
    return x * x + y * y;
  }

  // Normalize

  public Vector2 normalize() {
    float mag = magnitude();
    if (mag == 0) {
      return ZERO;
    }
    return new Vector2(x / mag, y / mag);
  }

  // Dot product

  public float dot(Vector2 other) {
    return x * other.x + y * other.y;
  }

  // Distance

  public float distance(Vector2 other) {
    return subtract(other).magnitude();
  }

  public float distanceSquared(Vector2 other) {
    float dx = other.x - x;
    float dy = other.y - y;
    return dx * dx + dy * dy;
  }

  // Angle (radians)

  public float angle() {
    return (float) Math.atan2(y, x);
  }

  // Rotate vector

  public Vector2 rotate(float radians) {
    float cos = (float) Math.cos(radians);
    float sin = (float) Math.sin(radians);

    return new Vector2(x * cos - y * sin, x * sin + y * cos);
  }

  // Clamp magnitude (limit speed)

  public Vector2 clampMagnitude(float maxLength) {
    float magSq = magnitudeSquared();

    if (magSq > maxLength * maxLength) {
      return normalize().scale(maxLength);
    }

    return this;
  }

  // Linear interpolation

  public Vector2 lerp(Vector2 target, float t) {
    return new Vector2(x + (target.x - x) * t, y + (target.y - y) * t);
  }

  public static Vector2 randomUnit() {
    double angle = Math.random() * Math.PI * 2;
    return new Vector2(
        (float) Math.cos(angle),
        (float) Math.sin(angle)
    );
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Vector2 v)) {
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
