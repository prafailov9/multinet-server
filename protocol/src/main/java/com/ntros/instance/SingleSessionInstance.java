package com.ntros.instance;

import com.ntros.model.world.connector.WorldConnector;
import com.ntros.session.Session;
import com.ntros.ticker.Ticker;

import java.util.Map;

public class SingleSessionInstance extends AbstractInstance {

    private final Session session;

    SingleSessionInstance(WorldConnector worldConnector, Ticker ticker, Session session) {
        super(worldConnector, ticker);
        this.session = session;
    }

    @Override
    public void run() {

    }

    @Override
    public String worldName() {
        return "";
    }

    @Override
    public void reset() {

    }

    @Override
    public void registerSession(Session session) {

    }

    @Override
    public void removeSession(Session session) {

    }

    @Override
    public Session getSession(Long sessionId) {
        return null;
    }

    @Override
    public int getActiveSessionsCount() {
        return 0;
    }

    @Override
    public boolean isRunning() {
        return false;
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
