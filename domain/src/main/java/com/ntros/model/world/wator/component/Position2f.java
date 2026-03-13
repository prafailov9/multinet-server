package com.ntros.model.world.wator.component;

/**
 * Continuous 2D position in world units.  Mutable so systems can update in-place
 * without allocating a new object every tick.
 */
public final class Position2f {

  public float x;
  public float y;

  public Position2f(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public void set(float x, float y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public String toString() {
    return String.format("(%.2f, %.2f)", x, y);
  }
}
