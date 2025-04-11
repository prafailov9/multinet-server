package com.ntros.model.world.engine.solid;

import com.ntros.model.entity.Entity;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.Result;
import com.ntros.model.world.state.WorldState;

public interface WorldEngine {

    void tick(WorldState worldState);

    Result storeMoveIntent(MoveRequest move, WorldState worldState);

    Result add(JoinRequest joinRequest, WorldState worldState);

    Entity remove(String entityId, WorldState worldState);

    String serialize(WorldState worldState);
}
