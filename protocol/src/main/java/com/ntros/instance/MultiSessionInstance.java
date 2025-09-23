package com.ntros.instance;

import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.session.Session;
import com.ntros.ticker.Ticker;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

/**
 * Layer that allows the world to interact with clients. Unique per world connector + session
 * manager.
 */

@Slf4j
public class MultiSessionInstance implements Instance {

    private final SessionManager sessionManager;
    private final WorldConnector worldConnector;
    private final Ticker ticker;
    private final AtomicBoolean tickerRunning = new AtomicBoolean(false);

    public MultiSessionInstance(WorldConnector worldConnector, SessionManager sessionManager, Ticker ticker) {
        this.worldConnector = worldConnector;
        this.sessionManager = sessionManager;
        this.ticker = ticker;
    }

    @Override
    public String worldName() {
        return worldConnector.worldName();
    }

    @Override
    public void run() {
        log.info("[IN WORLD INSTANCE]: Updating {} state...", worldName());
        ticker.tick(() -> {
            worldConnector.update();

            String stateMessage = "STATE " + worldConnector.serialize();
            log.info("Broadcasting server response to clients:\n {}", stateMessage);

            sessionManager.broadcast(stateMessage);
        });
        tickerRunning.set(true);
    }

    @Override
    public void reset() {
        log.info("Resetting World {}... stopping active sessions.", worldName());
        ticker.shutdown();
        tickerRunning.set(false);
        sessionManager.shutdownAll();
        worldConnector.reset();
    }

    @Override
    public void registerSession(Session session) {
        sessionManager.register(session);
    }

    @Override
    public void removeSession(Session session) {
        sessionManager.remove(session);
    }

    @Override
    public Session getSession(Long sessionId) {
        return sessionManager.getActiveSessions()
                .stream()
                .filter(session -> session.getProtocolContext().getSessionId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Session with ID:[%s] does not exist", sessionId)));
    }

    @Override
    public int getActiveSessionsCount() {
        return sessionManager.activeSessionsCount();
    }

    @Override
    public boolean isRunning() {
        return tickerRunning.get();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void updateTickRate(int ticksPerSecond) {

    }

    @Override
    public void updateWorldState(Map<Boolean, Boolean> worldStateUpdates) {

    }


}
