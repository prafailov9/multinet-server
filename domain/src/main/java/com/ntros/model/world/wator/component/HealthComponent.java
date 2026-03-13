package com.ntros.model.world.wator.component;

/**
 * Hit points for a creature.
 *
 * <p>Health decays when {@link EnergyComponent#isDepleted()} is true (starvation),
 * and when another creature deals damage during a combat interaction.
 * Reaching zero triggers entity death.
 */
public final class HealthComponent {

  public float current;
  public final float max;

  /** HP lost per tick while starving (energy == 0). */
  public static final float STARVATION_DECAY = 1.5f;

  /** Base damage a predator deals to prey per attack tick. */
  public static final float PREDATOR_ATTACK_DAMAGE = 20f;

  /** Base damage prey deal back to a predator (self-defence). */
  public static final float PREY_DEFENCE_DAMAGE = 5f;

  /** Interaction distance: entities closer than this can interact (eat / attack). */
  public static final float INTERACTION_RADIUS = 6f;

  public HealthComponent(float initial, float max) {
    this.current = initial;
    this.max     = max;
  }

  public boolean isDead() {
    return current <= 0f;
  }

  public void damage(float amount) {
    current = Math.max(0f, current - amount);
  }

  public void heal(float amount) {
    current = Math.min(max, current + amount);
  }
}
