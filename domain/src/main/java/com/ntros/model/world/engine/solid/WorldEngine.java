package com.ntros.model.world.engine.solid;

import com.ntros.model.entity.Entity;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.state.WorldState;

/**
 * Abstraction that manipulates a given world state.
 */
public interface WorldEngine {


  /**
   * Executes all stored player intents.
   *
   * @param worldState - the world to apply the intents to.
   */
  void applyIntents(WorldState worldState);

  CommandResult storeMoveIntent(MoveRequest move, WorldState worldState);

  CommandResult joinEntity(JoinRequest joinRequest, WorldState worldState);

  Entity removeEntity(String entityId, WorldState worldState);

  /**
   * Serializes the given world state to a json string
   *
   * @param worldState - state to serialize
   * @return json world state
   */
  String serialize(WorldState worldState);

  String serializeOneLine(WorldState worldState);

  void reset(WorldState worldState);
}
