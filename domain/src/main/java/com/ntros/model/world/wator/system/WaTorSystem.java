package com.ntros.model.world.wator.system;

import com.ntros.model.world.wator.WaTorWorldState;

/**
 * One step in the Wa-Tor ECS pipeline. Each system is called once per tick in the
 * order registered in {@link com.ntros.model.world.wator.WaTorWorld}.
 */
@FunctionalInterface
public interface WaTorSystem {

  /**
   * @param state mutable ECS world state
   * @param dt    elapsed time since last tick in seconds
   */
  void tick(WaTorWorldState state, float dt);
}
