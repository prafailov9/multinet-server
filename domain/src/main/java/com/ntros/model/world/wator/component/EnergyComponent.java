package com.ntros.model.world.wator.component;

/**
 * Energy reserve for creatures, or nutrition value for Food entities.
 *
 * <h3>For PREDATOR / PREY</h3>
 * {@link #current} is drained by movement ({@code speed² × MOVEMENT_COST_FACTOR} per tick)
 * and replenished by eating.  When it hits zero, {@link HealthComponent} begins to decay.
 *
 * <h3>For FOOD</h3>
 * {@link #current} holds the nutrition value granted to the predator that eats it.
 * {@link #max} holds the initial nutrition value for reference.
 * A separate {@code ttl} counter (managed by {@code DeathSystem}) controls how long
 * food persists before despawning.
 */
public final class EnergyComponent {

  public float current;
  public final float max;

  /** Energy drained per (world-units/sec)² per tick. */
  public static final float MOVEMENT_COST_FACTOR = 0.0004f;

  /** Base passive drain per tick even when stationary. */
  public static final float IDLE_DRAIN = 0.05f;

  /** Energy restored when prey eats a plant (fraction of plant's nutrition). */
  public static final float PLANT_NUTRITION = 30f;

  /** Energy restored when predator eats prey (fraction of prey's current energy). */
  public static final float PREY_NUTRITION_FRACTION = 0.6f;

  public EnergyComponent(float initial, float max) {
    this.current = initial;
    this.max     = max;
  }

  public boolean isDepleted() {
    return current <= 0f;
  }

  public void drain(float amount) {
    current = Math.max(0f, current - amount);
  }

  public void replenish(float amount) {
    current = Math.min(max, current + amount);
  }
}
