package com.ntros.model.world.engine.core;

import com.ntros.model.entity.Entity;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.PlayerGridState;

public interface PlayerGridEngine extends GridEngine {

  WorldResult joinEntity(JoinRequest req, PlayerGridState state);

  WorldResult storeMoveIntent(MoveRequest req, PlayerGridState state);

  Entity removeEntity(String entityId, PlayerGridState state);

}
