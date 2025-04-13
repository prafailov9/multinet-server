package com.ntros.model.world.connector;

import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.Result;

/**
 * Layer, connecting clients to the actual game world. Exposes minimal contract for client interaction with the world.
 * Unique per engine + state.
 */
public interface WorldConnector {
    void update();

    Result storeMoveIntent(MoveRequest move);

    Result add(JoinRequest joinRequest);

    void remove(String entityId);

    String serialize();

    String worldName();
}
