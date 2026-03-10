package com.ntros;


import com.ntros.config.converter.WorldConverter;
import com.ntros.config.reader.WorldConfigReader;
import com.ntros.event.broadcaster.SessionsBroadcaster;
import com.ntros.event.sessionmanager.ClientSessionManager;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.clock.FixedRateClock;
import com.ntros.lifecycle.clock.PacedRateClock;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.instance.ServerInstance;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.server.Server;
import com.ntros.server.TcpServer;
import java.io.IOException;
import java.util.List;
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

    // load default worlds from worlds.yml and init instances
    List<WorldConnector> worlds = loadWorlds();
    initInstances(worlds);
    Server server = new TcpServer(PORT);

    try {
      server.start();
    } catch (IOException ex) {
      log.error("Server failed: {}", ex.getMessage());

    }

  }

  private static List<WorldConnector> loadWorlds() {
    WorldConverter converter = new WorldConverter();
    return new WorldConfigReader().readAll().stream().map(converter::toModelObject).toList();
  }

  private static void initInstances(List<WorldConnector> worlds) {
    for (WorldConnector world : worlds) {
      SessionManager sessionManager = new ClientSessionManager();
      // build clock based on worlds cap:
      Clock clock = new PacedRateClock(TICK_RATE); // default if cap cfg missing
      // if singleplayer: fixed clock
      if (!world.getCapabilities().supportsPlayers()) {
        clock = new FixedRateClock(TICK_RATE);
      }

      Instances.registerInstance(
          new ServerInstance(world, sessionManager, clock, new SessionsBroadcaster(),
              Settings.multiplayer(BROADCAST_RATE)));
    }
  }

}
