package com.ntros.model.entity.movement.velocity;

import com.ntros.model.entity.movement.vectors.Vector2;
import java.util.Objects;

public class Velocity2 implements Velocity {

  private final float dx;
  private final float dy;

  private Velocity2(float dx, float dy) {
    this.dx = dx;
    this.dy = dy;
  }

  public static Velocity2 of(float dx, float dy) {
    return new Velocity2(dx, dy);
  }

  @Override
  public float getDx() {
    return dx;
  }

  @Override
  public float getDy() {
    return dy;
  }

  public Velocity2 add(Vector2 other) {
    return new Velocity2(this.dx + other.getX(), this.dy + other.getY());
  }

  public Velocity2 scale(float factor) {
    return new Velocity2(this.dx * factor, this.dy * factor);
  }

  public Velocity2 normalize() {
    float lenSq = dx * dx + dy * dy;
    if (lenSq == 0f) {
      return this;
    }

    float invLen = quake3InverseSqrt(lenSq);
    return new Velocity2(dx * invLen, dy * invLen);
  }


  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Velocity2 that)) {
      return false;
    }
    return Float.compare(dx, that.dx) == 0 && Float.compare(dy, that.dy) == 0;
  }


  private float magnitude() {
    float lengthSquared = dx * dx + dy * dy;

    if (lengthSquared == 0f) {
      return 0f;
    }

    return quake3InverseSqrt(lengthSquared);
  }

  private float fastSqrt(float x) {
    if (x <= 0) {
      return 0;
    }
    float guess = x;
    guess = 0.5f * (guess + x / guess); // 1st iteration
    // guess = 0.5f * (guess + x / guess); // Optional 2nd iteration for better accuracy
    return guess;
  }

  /**
   * the Quake 3 meme
   */
  private float quake3InverseSqrt(float x) {
    float xHalf = 0.5f * x;
    int i = Float.floatToIntBits(x);
    i = 0x5f3759df - (i >> 1);
    x = Float.intBitsToFloat(i);
    x = x * (1.5f - xHalf * x * x); // 1st Newton-Raphson iteration
    x = x * (1.5f - xHalf * x * x); // optional 2nd iteration
    return 1 / x;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dx, dy);
  }

  @Override
  public String toString() {
    return "Velocity2D{" + "dx=" + dx + ", dy=" + dy + '}';
  }
}
