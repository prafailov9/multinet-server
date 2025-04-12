package com.ntros;

import com.ntros.event.bus.EventBus;
import com.ntros.event.bus.SessionEventBus;
import com.ntros.event.listener.*;
import com.ntros.model.world.WorldDispatcher;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.runtime.Instance;
import com.ntros.runtime.WorldInstance;
import com.ntros.server.Server;
import com.ntros.server.TcpServer;
import com.ntros.server.scheduler.WorldTickScheduler;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerBootstrap {

    private static final Logger LOGGER = Logger.getLogger(ServerBootstrap.class.getName());
    private static final int PORT = 5555;
    private static final int TICK_RATE = 120; // 10 ticks per second

    public static void startServer() {
        LOGGER.log(Level.INFO, "Starting server on port " + PORT);

        // create event bus
        EventBus eventBus = new SessionEventBus();
        SessionManager sessionManager = new ServerSessionManager();

        // creating default world
        WorldConnector world = WorldDispatcher.getDefaultWorld();
        Instance instance = new WorldInstance(world, sessionManager);

        WorldTickScheduler tickScheduler = new WorldTickScheduler(TICK_RATE);
        tickScheduler.register(instance);

        // registering the connection event listener
        SessionEventListener connectionEventListener = new ConnectionEventListener(sessionManager, tickScheduler);

        Server server = create(eventBus, sessionManager, connectionEventListener);

        try {
            server.start(PORT, eventBus);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Server failed: {}", ex.getMessage());

        }

    }

    private static Server create(EventBus eventBus, SessionManager serverSessionManager, SessionEventListener connectionEventListener) {
        SessionEventListener sessionCleaner = new SessionCleaner();
        eventBus.register(sessionCleaner);

        return new TcpServer(serverSessionManager, connectionEventListener);
    }

}
