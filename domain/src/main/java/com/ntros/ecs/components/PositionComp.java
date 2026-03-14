package com.ntros.ecs.components;

import com.ntros.ecs.core.Component;

/**
 * Continuous 2-D position in world-space units.
 *
 * <p>Stored in a {@code DenseStore} — every boid has a position.
 */
public record PositionComp(float x, float y) implements Component {

}
