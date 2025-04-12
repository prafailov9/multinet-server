package com.ntros.model.world.connector;

import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.Result;

public interface WorldConnector {
    void tick();
    Result storeMoveIntent(MoveRequest move);
    Result add(JoinRequest joinRequest);
    void remove(String entityId);
    String serialize();
    String worldName();
}
