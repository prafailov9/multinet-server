package com.ntros.model.world.engine.core;

import com.ntros.model.entity.Entity;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.state.core.GridState;

/**
 * Contract for all grid-based world engines.
 *
 * <p>Implementations receive a mutable {@link GridState} on every call and may freely mutate it;
 * thread-safety is guaranteed by the caller ({@link com.ntros.model.world.connector.GridWorldConnector}
 * always dispatches through the single-threaded {@code CommandActor}).
 *
 * @see com.ntros.model.world.engine.solid.GridWorldEngine
 * @see com.ntros.model.world.engine.gameoflife.GridGolEngine
 */
public interface GridEngine {

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
   * non-orchestrated engines (e.g. {@link com.ntros.model.world.engine.solid.GridWorldEngine})
   * do not need to override this method.
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

  Object snapshot(GridState state);

  String serializeOneLine(GridState state);

  void reset(GridState state);
}
