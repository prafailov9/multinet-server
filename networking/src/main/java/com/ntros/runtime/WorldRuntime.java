package com.ntros.runtime;

import com.ntros.model.world.context.WorldContext;
import com.ntros.session.event.SessionManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WorldRuntime implements Runtime {
    private static final Logger LOGGER = Logger.getLogger(WorldRuntime.class.getName());

    private final WorldContext worldContext;
    private final SessionManager sessionManager;

    public WorldRuntime(WorldContext worldContext, SessionManager sessionManager) {
        this.worldContext = worldContext;
        this.sessionManager = sessionManager;
    }


    @Override
    public String getWorldName() {
        return worldContext.state().worldName();
    }

    @Override
    public void run() {
        System.out.println("[WorldRuntime] World: " + worldContext.state().worldName() + " ticked and broadcasting state.");

        LOGGER.log(Level.INFO, "Updating {0} state...", getWorldName());
        worldContext.engine().tick(worldContext.state());
        String stateMessage = "STATE " + worldContext.engine().serialize(worldContext.state());
        LOGGER.log(Level.INFO, "Broadcasting server response to clients:\n {0}", stateMessage);

        sessionManager.broadcast(stateMessage);
    }
}
