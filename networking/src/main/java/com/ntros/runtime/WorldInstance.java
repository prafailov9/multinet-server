package com.ntros.runtime;

import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.connector.WorldConnector;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Layer that allows the world to interact with clients. Unique per world connector + session manager.
 */
public class WorldInstance implements Instance {
    private static final Logger LOGGER = Logger.getLogger(WorldInstance.class.getName());
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
        LOGGER.log(Level.INFO, "Updating {0} state...", getWorldName());
        worldConnector.tick();

        String stateMessage = "STATE " + worldConnector.serialize();
        LOGGER.log(Level.INFO, "Broadcasting server response to clients:\n {0}", stateMessage);

        sessionManager.broadcast(stateMessage);
    }


}
