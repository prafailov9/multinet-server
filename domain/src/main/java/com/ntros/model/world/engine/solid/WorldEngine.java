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

  void tick(WorldState worldState);

  CommandResult storeMoveIntent(MoveRequest move, WorldState worldState);

  CommandResult add(JoinRequest joinRequest, WorldState worldState);

  Entity remove(String entityId, WorldState worldState);

  String serialize(WorldState worldState);

  String serializeOneLine(WorldState worldState);

  void reset(WorldState worldState);
}
