package com.ntros.model.world.wator.component;

/**
 * Polar-form velocity for a creature.
 *
 * <p>The neural network outputs angular and linear velocity directly, so storing them in
 * polar form avoids a redundant conversion on every tick.  The {@link MovementSystem} converts
 * to Cartesian deltas only when integrating into {@link Position2f}.
 *
 * <ul>
 *   <li>{@link #angle}       — heading in radians (0 = right, π/2 = down in canvas space).</li>
 *   <li>{@link #speed}       — current linear speed in world-units per second.  Always ≥ 0.</li>
 *   <li>{@link #angularVel}  — turn rate in radians per second (output from NN, applied each tick).</li>
 * </ul>
 */
public final class VelocityComponent {

  public float angle;
  public float speed;
  public float angularVel;

  public VelocityComponent(float angle, float speed) {
    this.angle      = angle;
    this.speed      = speed;
    this.angularVel = 0f;
  }

  /** Maximum linear speed cap (world-units / second). */
  public static final float MAX_SPEED = 60f;

  /** Maximum turn rate cap (radians / second). */
  public static final float MAX_ANGULAR_VEL = (float) (Math.PI * 2);
}
