package com.ntros.instance;

import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.server.scheduler.TickScheduler;
import lombok.extern.slf4j.Slf4j;

/**
 * Layer that allows the world to interact with clients. Unique per world connector + session manager.
 */
@Slf4j
public class WorldInstance implements Instance {
    private final WorldConnector worldConnector;
    private final SessionManager sessionManager;
    private TickScheduler tickScheduler;

    public WorldInstance(WorldConnector worldConnector, SessionManager sessionManager) {
        this.worldConnector = worldConnector;
        this.sessionManager = sessionManager;
    }

    public WorldInstance(WorldConnector worldConnector, SessionManager sessionManager, TickScheduler tickScheduler) {
        this.worldConnector = worldConnector;
        this.sessionManager = sessionManager;
        this.tickScheduler = tickScheduler;
    }

    @Override
    public String worldName() {
        return worldConnector.worldName();
    }

    @Override
    public void run() {
        log.info("[IN WORLD INSTANCE]: Updating {} state...", worldName());
        tickScheduler.tick(() -> {
            worldConnector.update();

            String stateMessage = "STATE " + worldConnector.serialize();
            log.info("Broadcasting server response to clients:\n {}", stateMessage);

            sessionManager.broadcast(stateMessage);
        });
    }

    @Override
    public void reset() {
        log.info("Resetting World {}... stopping active sessions.", worldName());
        tickScheduler.shutdown();
        sessionManager.shutdownAll();
        worldConnector.reset();
    }


}
