package com.ntros.model.world.state.core;

import com.ntros.model.entity.movement.vectors.Vector3;
import com.ntros.model.entity.open.OpenWorldEntity;
import com.ntros.model.world.state.dimension.Dimension3D;
import java.util.Map;

/**
 * Mutable state container for a continuous 3D open world.
 *
 * <p>All maps are owned by the state object and mutated exclusively on the actor (engine) thread.
 * The {@code moveIntents} map accumulates thrust vectors submitted by player sessions between
 * ticks; the engine drains it during {@code applyIntents}.
 */
public interface OpenWorldState extends CoreState {

  /**
   * Bounding volume of the world.
   */
  Dimension3D dimension();

  /**
   * Live entity registry. Key = {@link OpenWorldEntity#getName()}.
   * Modified only by the engine.
   */
  Map<String, OpenWorldEntity> entities();

  /**
   * Pending movement intents queued since the last tick.
   * Key = entity name; value = normalised thrust direction.
   * Cleared by the engine at the end of each {@code applyIntents} call.
   */
  Map<String, Vector3> moveIntents();
}
