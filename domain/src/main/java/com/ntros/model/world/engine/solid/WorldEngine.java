package com.ntros.model.world.engine.solid;

import com.ntros.model.entity.Entity;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.state.GridState;

/**
 * Abstraction that manipulates a given world state.
 */
public interface WorldEngine {

  /**
   * Executes all stored intents for the current tick.
   * For player-driven worlds this applies move intents;
   * for orchestrated worlds (e.g. Game of Life) this advances the simulation.
   *
   * @param state the world to apply intents to
   */
  void applyIntents(GridState state);

  WorldResult storeMoveIntent(MoveRequest move, GridState state);

  WorldResult joinEntity(JoinRequest joinRequest, GridState state);

  Entity removeEntity(String entityId, GridState state);

  /**
   * Handles an orchestrator command (seed, toggle, clear, random-seed).
   *
   * <p>The default implementation returns a "not supported" failure so that
   * non-orchestrated engines (e.g. {@link GridWorldEngine}) do not need to
   * override this method.
   */
  default WorldResult orchestrate(OrchestrateRequest req, GridState state) {
    return WorldResult.failed("orchestrator", state.worldName(),
        "This world does not support orchestration.");
  }

  /**
   * Serializes the given world state to a JSON string.
   *
   * @param state state to serialize
   * @return json world state
   */
  String serialize(GridState state);

  String serializeOneLine(GridState state);

  void reset(GridState state);
}
