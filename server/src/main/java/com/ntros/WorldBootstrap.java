package com.ntros;

import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.WorldDispatcher;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.runtime.Instance;
import com.ntros.runtime.WorldInstance;
import com.ntros.server.scheduler.WorldTickScheduler;

public class WorldBootstrap {
    private static final int TICK_RATE = 10; // 10 ticks per second
    private final static WorldTickScheduler worldTickScheduler = new WorldTickScheduler(TICK_RATE);
    public static void initializeWorld(SessionManager sessionManager) {
        // creating default world
        WorldConnector world = WorldDispatcher.getDefaultWorld();
        Instance instance = new WorldInstance(world, sessionManager);

        // start heartbeat
        WorldTickScheduler tickScheduler = new WorldTickScheduler(TICK_RATE);
        worldTickScheduler.register(instance);

        // run only when there is a client registered
        worldTickScheduler.start();
    }

    public static void shutdownWorld(SessionManager sessionManager) {
        worldTickScheduler.shutdown();
    }

}
