package com.ntros;

import com.ntros.event.broadcaster.BroadcastToAll;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.clock.PacedRateClock;
import com.ntros.model.entity.config.access.Settings;
import java.io.IOException;

import com.ntros.event.sessionmanager.ClientSessionManager;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.instance.ServerInstance;
import com.ntros.model.world.Connectors;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.server.Server;
import com.ntros.server.TcpServer;
import com.ntros.lifecycle.clock.FixedRateClock;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerBootstrap {

  // Default port
  private static final int PORT = 5555;

  // server updates 120 times per second
  private static final int TICK_RATE = 120;
  // emits only 70 messages per second
  private static final int BROADCAST_RATE = 70;

  public static void startServer() {
    log.info("Starting server on port {}", PORT);

    // init default worlds
    initWorld("world-1", new PacedRateClock(TICK_RATE));
    initWorld("world-2", new PacedRateClock(TICK_RATE));
    initWorld("world-3", new PacedRateClock(TICK_RATE));
    initWorld("arena-x", new PacedRateClock(TICK_RATE));
    initWorld("arena-y", new PacedRateClock(TICK_RATE));

    Server server = new TcpServer(PORT);

    try {
      server.start();
    } catch (IOException ex) {
      log.error("Server failed: {}", ex.getMessage());

    }

  }

  private static void initWorld(String name, Clock clock) {
    WorldConnector world = Connectors.getWorld(name);
    SessionManager sessionManager = new ClientSessionManager();

    ServerInstance instance = new ServerInstance(world, sessionManager, clock,
        new BroadcastToAll(),
        Settings.multiplayer(BROADCAST_RATE));
    Instances.registerInstance(instance);
  }
}
