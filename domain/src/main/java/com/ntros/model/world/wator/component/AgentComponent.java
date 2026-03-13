package com.ntros.model.world.wator.component;

/**
 * Marks an entity as an autonomous agent (predator or prey) and carries its
 * reproduction-readiness state.
 *
 * <p>Agents accumulate {@link #reproductionProgress} over time when they have surplus energy.
 * When it reaches {@link #reproductionThreshold} the entity queues an offspring spawn and
 * resets the counter.
 */
public final class AgentComponent {

  /** Accumulated progress toward next reproduction event (0 → reproductionThreshold). */
  public float reproductionProgress;

  /** Energy fraction above which the agent accrues reproduction progress. */
  public static final float SURPLUS_ENERGY_FRACTION = 0.65f;

  /** Ticks of surplus energy required to reproduce. */
  public static final float REPRODUCTION_THRESHOLD = 200f;

  public AgentComponent() {
    this.reproductionProgress = 0f;
  }
}
