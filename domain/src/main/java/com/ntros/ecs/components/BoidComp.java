package com.ntros.ecs.components;

import com.ntros.ecs.core.Component;

/**
 * Boid steering-behaviour radii.
 *
 * <ul>
 *   <li>{@code sepRadius} — push away from neighbours closer than this.</li>
 *   <li>{@code alignRadius} — match velocity with neighbours within this radius.</li>
 *   <li>{@code cohRadius} — steer toward the centre of mass of neighbours within this
 *       radius.</li>
 * </ul>
 *
 * <p>Stored in a {@code DenseStore} — every boid has steering parameters.
 */
public record BoidComp(float sepRadius, float alignRadius, float cohRadius) implements Component {

  /** Default boid with Reynolds-tuned radii suitable for a 100×100 world. */
  public static BoidComp defaults() {
    return new BoidComp(4f, 10f, 10f);
  }
}
