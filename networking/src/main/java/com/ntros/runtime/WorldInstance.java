package com.ntros.runtime;

import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.connector.WorldConnector;
import lombok.extern.slf4j.Slf4j;

/**
 * Layer that allows the world to interact with clients. Unique per world connector + session manager.
 */
@Slf4j
public class WorldInstance implements Instance {
    private final WorldConnector worldConnector;
    private final SessionManager sessionManager;

    public WorldInstance(WorldConnector worldConnector, SessionManager sessionManager) {
        this.worldConnector = worldConnector;
        this.sessionManager = sessionManager;
    }

    @Override
    public String getWorldName() {
        return worldConnector.worldName();
    }

    @Override
    public void run() {
        log.info("Updating {} state...", getWorldName());
        worldConnector.update();

        String stateMessage = "STATE " + worldConnector.serialize();
        log.info("Broadcasting server response to clients:\n {}", stateMessage);

        sessionManager.broadcast(stateMessage);
    }


}
