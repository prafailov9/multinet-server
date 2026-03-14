package com.ntros.ecs.components;

import com.ntros.ecs.core.Component;

/**
 * 2-D velocity in world-space units per second.
 *
 * <p>Stored in a {@code DenseStore} — every boid has a velocity.
 */
public record VelocityComp(float vx, float vy) implements Component {

}
