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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ServerBootstrap {

    private static final int PORT = 5555;
    private static final int TICK_RATE = 120; // 120 ticks per second

    public static void startServer() {
        log.info("Starting server on port {}", PORT);
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
            log.error("Server failed: {}", ex.getMessage());

        }

    }

    private static Server create(EventBus eventBus, SessionManager serverSessionManager, SessionEventListener connectionEventListener) {
        SessionEventListener sessionCleaner = new SessionCleaner();
        eventBus.register(sessionCleaner);

        return new TcpServer(serverSessionManager, connectionEventListener);
    }

}
