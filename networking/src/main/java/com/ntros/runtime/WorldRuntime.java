package com.ntros.runtime;

import com.ntros.model.world.WorldContext;
import com.ntros.session.SessionManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WorldRuntime implements Runtime {
    private static final Logger LOGGER = Logger.getLogger(WorldRuntime.class.getName());

    private final WorldContext world;
    private final SessionManager sessionManager;

    public WorldRuntime(WorldContext world, SessionManager sessionManager) {
        this.world = world;
        this.sessionManager = sessionManager;
    }


    @Override
    public String getWorldName() {
        return world.name();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public WorldContext getWorldContext() {
        return world;
    }

    @Override
    public void run() {
        System.out.println("[WorldRuntime] World: " + world.name() + " ticked and broadcasting state.");

        LOGGER.log(Level.INFO, "Updating {0} state...", getWorldName());
        world.tick();
        String stateMessage = "STATE " + world.serialize();
        LOGGER.log(Level.INFO, "Broadcasting server response to clients:\n {0}", stateMessage);

        sessionManager.broadcast(stateMessage);
    }
}
