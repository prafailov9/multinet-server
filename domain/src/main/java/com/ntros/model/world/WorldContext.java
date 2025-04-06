package com.ntros.model.world;

import com.ntros.model.entity.Position;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.Result;

public interface WorldContext {

    String name();

    Result add(JoinRequest joinRequest);

    void remove(String entityId);

    void tick();

    Result storeMoveIntent(MoveRequest moveRequest);

    String serialize();

    boolean isLegalMove(Position position);

}
