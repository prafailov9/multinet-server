package com.ntros.model.world.engine.solid;

import com.ntros.model.entity.Entity;
import com.ntros.model.world.protocol.ServerResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.state.GridState;

/**
 * Abstraction that manipulates a given world state.
 */
public interface WorldEngine {


  /**
   * Executes all stored player intents.
   *
   * @param state - the world to apply the intents to.
   */
  void applyIntents(GridState state);

  ServerResult storeMoveIntent(MoveRequest move, GridState state);

  ServerResult joinEntity(JoinRequest joinRequest, GridState state);

  Entity removeEntity(String entityId, GridState state);

  /**
   * Serializes the given world state to a json string
   *
   * @param state - state to serialize
   * @return json world state
   */
  String serialize(GridState state);

  String serializeOneLine(GridState state);

  void reset(GridState state);
}
