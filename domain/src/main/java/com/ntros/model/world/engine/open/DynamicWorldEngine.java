package com.ntros.model.world.engine.open;

import com.ntros.model.entity.open.OpenWorldEntity;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.OpenMoveRequest;
import com.ntros.model.world.state.open.DynamicWorldState;

/**
 * Engine contract for a continuous 3D open world.
 *
 * <p>Mirrors the structure of {@link com.ntros.model.world.engine.solid.WorldEngine} for grid
 * worlds, adapted for physics-driven movement and float-precision positions.
 */
public interface DynamicWorldEngine {

  /**
   * Drains all pending movement intents and integrates them into entity velocities and positions.
   *
   * @param state     mutable world state
   * @param deltaTime elapsed time since the last tick, in seconds
   */
  void applyIntents(DynamicWorldState state, float deltaTime);

  /**
   * Stages a movement intent submitted by the session layer.
   * The intent is stored and applied on the next {@link #applyIntents} call.
   *
   * @param req   the player's free-movement request
   * @param state mutable world state
   * @return result indicating whether the intent was accepted
   */
  WorldResult storeMoveIntent(OpenMoveRequest req, DynamicWorldState state);

  /**
   * Spawns a new player entity at a random position within the world bounds.
   *
   * @param req   join request carrying the player name
   * @param state mutable world state
   * @return result indicating success (with spawn coordinates) or failure
   */
  WorldResult joinEntity(JoinRequest req, DynamicWorldState state);

  /**
   * Removes the named entity from the world and returns it.
   *
   * @param entityName the {@link OpenWorldEntity#getName()} key
   * @param state      mutable world state
   * @return the removed entity, or {@code null} if not found
   */
  OpenWorldEntity removeEntity(String entityName, DynamicWorldState state);

  /**
   * Serialises the current world state to pretty-printed JSON.
   *
   * @param state world state to serialise
   * @return multi-line JSON string
   */
  String serialize(DynamicWorldState state);

  /**
   * Serialises the current world state to a single-line JSON string (for wire transport).
   *
   * @param state world state to serialise
   * @return single-line JSON string
   */
  String serializeOneLine(DynamicWorldState state);

  /**
   * Resets the world to a clean (empty) state.
   *
   * @param state world state to clear
   */
  void reset(DynamicWorldState state);
}
