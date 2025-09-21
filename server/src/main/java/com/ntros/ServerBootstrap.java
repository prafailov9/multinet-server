package com.ntros;

import java.io.IOException;

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
import com.ntros.ticker.WorldTicker;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerBootstrap {

  private static final int PORT = 5555;
  private static final int TICK_RATE = 120; // 120 ticks per second

  public static void startServer() {
    log.info("Starting server on port {}", PORT);

    initWorld("world-1", new WorldTicker(TICK_RATE));
    initWorld("world-2", new WorldTicker(TICK_RATE));
    initWorld("arena-x", new WorldTicker(TICK_RATE));
    initWorld("arena-y", new WorldTicker(TICK_RATE));
    initWorld("arena-z", new WorldTicker(TICK_RATE));

    Server server = new TcpServer(PORT);

    try {
      server.start();
    } catch (IOException ex) {
      log.error("Server failed: {}", ex.getMessage());

    }

  }

  private static void initWorld(String name, WorldTicker scheduler) {
    WorldConnector world = WorldConnectorHolder.getWorld(name);
    SessionManager sessionManager = new ClientSessionManager();

    WorldInstance instance = new WorldInstance(world, sessionManager, scheduler);
    InstanceRegistry.register(instance);

    // Register the per-world listener
    SessionEventListener instanceListener = new InstanceSessionEventListener(instance);
    SessionEventBus.get().register(instanceListener);
  }

}
