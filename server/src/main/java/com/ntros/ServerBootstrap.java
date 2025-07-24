package com.ntros;

import com.ntros.event.bus.SessionEventBus;
import com.ntros.event.listener.ClientSessionEventListener;
import com.ntros.event.listener.ClientSessionManager;
import com.ntros.event.listener.SessionEventListener;
import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.instance.InstanceRegistry;
import com.ntros.instance.WorldInstance;
import com.ntros.server.Server;
import com.ntros.server.TcpServer;
import com.ntros.server.scheduler.ServerTickScheduler;
import com.ntros.server.scheduler.WorldTickScheduler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ServerBootstrap {

    private static final int PORT = 5555;
    private static final int TICK_RATE = 120; // 120 ticks per second

    public static void startServer() {
        log.info("Starting server on port {}", PORT);
        // create tick scheduler
        // TODO: try refactor scheduler per world instance
        WorldTickScheduler scheduler = new WorldTickScheduler(TICK_RATE);

        initWorld("world-1", new ServerTickScheduler(TICK_RATE));
        initWorld("world-2", new WorldTickScheduler(TICK_RATE));
        initWorld("arena-x", new WorldTickScheduler(TICK_RATE));
        initWorld("arena-y", new WorldTickScheduler(TICK_RATE));
        initWorld("arena-z", new WorldTickScheduler(TICK_RATE));

        Server server = new TcpServer(scheduler);

        try {
            server.start(PORT);
        } catch (IOException ex) {
            log.error("Server failed: {}", ex.getMessage());

        }

    }

    private static void initWorld(String name, WorldTickScheduler scheduler) {
        WorldConnector world = WorldConnectorHolder.getWorld(name);
        SessionManager sessionManager = new ClientSessionManager();

        WorldInstance instance = new WorldInstance(world, sessionManager);
        InstanceRegistry.register(instance);
        scheduler.register(instance);

        // Register the per-world listener
        SessionEventListener listener = new ClientSessionEventListener(sessionManager, scheduler);
        SessionEventBus.get().register(listener);

    }

    private static void initWorld(String name, ServerTickScheduler scheduler) {
        WorldConnector world = WorldConnectorHolder.getWorld(name);
        SessionManager sessionManager = new ClientSessionManager();

        WorldInstance instance = new WorldInstance(world, sessionManager);
        InstanceRegistry.register(instance);

        // Register the per-world listener
        SessionEventListener listener = new ClientSessionEventListener(sessionManager, scheduler);
        SessionEventBus.get().register(listener);

    }

}
