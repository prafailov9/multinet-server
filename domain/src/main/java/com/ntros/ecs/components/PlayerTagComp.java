package com.ntros.ecs.components;

import com.ntros.ecs.core.Component;

/**
 * Tags an entity as being controlled by (or associated with) a specific player session.
 *
 * <p>Stored in a {@code SparseSet} — only a handful of observer entities carry this tag
 * compared to the overall boid population.
 */
public record PlayerTagComp(String playerId) implements Component {

}
