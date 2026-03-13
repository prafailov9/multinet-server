package com.ntros.model.world.wator.component;

/**
 * State for a Plant entity.
 *
 * <p>Plants grow passively each tick, spread to adjacent empty space once they reach
 * {@link #MAX_SIZE}, and provide nutrition to Prey that eat them.
 * They require no external resources.
 */
public final class PlantComponent {

  /** Ticks elapsed since last spread event. */
  public int ticksSinceLastSpread;

  /** Current growth stage (1–MAX_SIZE). Increases by {@link #GROWTH_RATE} per tick. */
  public float size;

  /** Maximum growth stage — once reached, the plant attempts to spread. */
  public static final float MAX_SIZE = 5f;

  /** Growth increment per tick. */
  public static final float GROWTH_RATE = 0.01f;

  /** Minimum ticks between spread attempts once at max size. */
  public static final int SPREAD_COOLDOWN_TICKS = 300;

  /** Search radius (world units) for an empty cell to spread into. */
  public static final float SPREAD_RADIUS = 40f;

  /** Probability that a new plant spontaneously appears anywhere in the world per tick. */
  public static final double SPONTANEOUS_SPAWN_CHANCE = 0.002;

  public PlantComponent(float initialSize) {
    this.size = initialSize;
    this.ticksSinceLastSpread = 0;
  }

  public boolean isFullyGrown() {
    return size >= MAX_SIZE;
  }
}
