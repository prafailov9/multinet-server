package com.ntros;

import com.ntros.model.world.WorldDispatcher;
import com.ntros.model.world.context.WorldContext;
import com.ntros.runtime.Runtime;
import com.ntros.runtime.WorldRuntime;
import com.ntros.server.Server;
import com.ntros.server.TcpServer;
import com.ntros.server.scheduler.WorldTickScheduler;
import com.ntros.session.event.SessionCleaner;
import com.ntros.session.event.SessionManager;
import com.ntros.session.event.bus.EventBus;
import com.ntros.session.event.bus.SessionEventBus;
import com.ntros.session.event.bus.SessionEventListener;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppBootstrap {

    private static final Logger LOGGER = Logger.getLogger(AppBootstrap.class.getName());
    private static final int PORT = 5555;
    private static final int TICK_RATE = 10; // 10 ticks per second

    public static void startServer() {
        LOGGER.log(Level.INFO, "Starting server on port " + PORT);

        EventBus eventBus = new SessionEventBus();
        SessionManager sessionManager = new SessionManager();
        Server server = create(eventBus, sessionManager);

        // start the server in its own thread.
        Thread.ofVirtual().start(() -> {
            try {
                server.start(PORT, eventBus);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Server failed: {}", ex.getMessage());

            }
        });

        // creating default world
        WorldContext world = WorldDispatcher.getDefaultWorld();
        Runtime runtime = new WorldRuntime(world, sessionManager);

        // start heartbeat
        WorldTickScheduler tickScheduler = new WorldTickScheduler(TICK_RATE);
        tickScheduler.register(runtime);
        tickScheduler.start();
    }

    private static Server create(EventBus eventBus, SessionManager sessionManager) {
        SessionEventListener sessionCleaner = new SessionCleaner();
        eventBus.register(sessionCleaner);

        return new TcpServer(sessionManager);
    }

}
