package com.ntros.model.world.engine.core;

import com.ntros.model.world.engine.sim.model.TrafficIntent;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.TrafficState;

/**
 * Pure traffic simulation logic. - Stores control inputs into the state (intents) - Advances the
 * world by one step (dt) - Serializes state for clients (JSON)
 * <p>
 * Threading: called only on the actor thread.
 */
public interface TrafficEngine {

  /**
   * Queue a driver/system intent; validated later during step.
   */
  WorldResult enqueueIntent(TrafficIntent intent, TrafficState state);

  /**
   * Advance simulation one step (apply intents, resolve signals, integrate motion, collisions,
   * routing).
   */
  void step(TrafficState state, double dtSeconds);

  /**
   * JSON (pretty) for debugging/tools.
   */
  String serialize(TrafficState state);

  /**
   * JSON (compact) for network.
   */
  String serializeOneLine(TrafficState state);

  /**
   * Reset to empty state (clear vehicles, timers, intents).
   */
  void reset(TrafficState state);
}

