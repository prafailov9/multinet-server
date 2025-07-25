package com.ntros.model.entity.movement;

import java.util.Objects;

public class Velocity2D implements Velocity {

  private final float dx;
  private final float dy;

  private Velocity2D(float dx, float dy) {
    this.dx = dx;
    this.dy = dy;
  }

  public static Velocity2D of(float dx, float dy) {
    return new Velocity2D(dx, dy);
  }

  @Override
  public float getDx() {
    return dx;
  }

  @Override
  public float getDy() {
    return dy;
  }

  public Velocity2D add(Vector2D other) {
    return new Velocity2D(this.dx + other.getX(), this.dy + other.getY());
  }

  public Velocity2D scale(float factor) {
    return new Velocity2D(this.dx * factor, this.dy * factor);
  }

  public Velocity2D normalize(Vector2D vector2D) {
    return null;
  }


  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Velocity2D that)) {
      return false;
    }
    return Float.compare(dx, that.dx) == 0 && Float.compare(dy, that.dy) == 0;
  }

  private float magnitude() {
    return 1f;
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
    return "Velocity2D{" +
        "dx=" + dx +
        ", dy=" + dy +
        '}';
  }
}
