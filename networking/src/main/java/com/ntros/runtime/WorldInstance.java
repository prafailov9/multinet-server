package com.ntros.runtime;

import com.ntros.event.SessionEvent;
import com.ntros.event.bus.SessionEventBus;
import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.connector.WorldConnector;
import lombok.extern.slf4j.Slf4j;

import static com.ntros.event.SessionEvent.sessionShutdownAll;

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
    public String worldName() {
        return worldConnector.worldName();
    }

    @Override
    public void reset() {
        log.info("Resetting World {}... stopping active sessions.", worldName());
        sessionManager.shutdownAll();
        worldConnector.reset();
    }

    @Override
    public void run() {
        log.info("[IN WORLD INSTANCE]: Updating {} state...", worldName());
        worldConnector.update();

        String stateMessage = "STATE " + worldConnector.serialize();
        log.info("Broadcasting server response to clients:\n {}", stateMessage);

        sessionManager.broadcast(stateMessage);
    }


}
