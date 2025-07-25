package com.ntros;

import com.ntros.event.bus.SessionEventBus;
import com.ntros.event.listener.ClientSessionManager;
import com.ntros.event.listener.InstanceSessionEventListener;
import com.ntros.event.listener.SessionEventListener;
import com.ntros.event.listener.SessionManager;
import com.ntros.instance.InstanceRegistry;
import com.ntros.instance.WorldInstance;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.server.Server;
import com.ntros.server.TcpServer;
import com.ntros.server.scheduler.ServerTickScheduler;
import com.ntros.server.scheduler.WorldTickScheduler;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

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
    initWorld("world-2", new ServerTickScheduler(TICK_RATE));
    initWorld("arena-x", new ServerTickScheduler(TICK_RATE));
    initWorld("arena-y", new ServerTickScheduler(TICK_RATE));
    initWorld("arena-z", new ServerTickScheduler(TICK_RATE));

    Server server = new TcpServer();

    try {
      server.start(PORT);
    } catch (IOException ex) {
      log.error("Server failed: {}", ex.getMessage());

    }

  }

  private static void initWorld(String name, ServerTickScheduler scheduler) {
    WorldConnector world = WorldConnectorHolder.getWorld(name);
    SessionManager sessionManager = new ClientSessionManager();

    WorldInstance instance = new WorldInstance(world, sessionManager, scheduler);
    InstanceRegistry.register(instance);

    // Register the per-world listener
    SessionEventListener instanceListener = new InstanceSessionEventListener(instance);
    SessionEventBus.get().register(instanceListener);
  }

}
