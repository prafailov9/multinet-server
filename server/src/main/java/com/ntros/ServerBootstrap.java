package com.ntros;

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
        SessionManager sessionManager = new ClientSessionManager();

        // creating default world
        WorldConnector world = WorldDispatcher.getDefaultWorld();
        Instance instance = new WorldInstance(world, sessionManager);

        WorldTickScheduler tickScheduler = new WorldTickScheduler(TICK_RATE);
        tickScheduler.register(instance);

        // registering the connection event listener
        SessionEventListener connectionEventListener = new ClientSessionEventListener(sessionManager, tickScheduler);

        Server server = create(sessionManager, connectionEventListener);

        try {
            server.start(PORT);
        } catch (IOException ex) {
            log.error("Server failed: {}", ex.getMessage());

        }

    }

    private static Server create(SessionManager serverSessionManager, SessionEventListener connectionEventListener) {
        SessionEventBus.get().register(new SessionCleaner());
        SessionEventBus.get().register(connectionEventListener);
        return new TcpServer(serverSessionManager);
    }

}
