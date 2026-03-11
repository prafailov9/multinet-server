package com.ntros;


import com.ntros.config.converter.WorldConverter;
import com.ntros.config.reader.WorldConfigReader;
import com.ntros.event.broadcaster.SharedBroadcaster;
import com.ntros.event.sessionmanager.ClientSessionManager;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.lifecycle.LifecycleHooks;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.clock.FixedRateClock;
import com.ntros.lifecycle.clock.PacedRateClock;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.instance.ServerInstance;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.state.solid.GridWorldState;
import com.ntros.persistence.db.ConnectionProvider;
import com.ntros.persistence.db.DatabaseBuilder;
import com.ntros.persistence.db.PersistenceContext;
import com.ntros.persistence.model.WorldRecord;
import com.ntros.server.Server;
import com.ntros.server.TcpServer;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerBootstrap {

  // Default port
  private static final int PORT = 5555;

  // server updates 120 times per second
  private static final int TICK_RATE = 120;
  // emits 70 messages per second
  private static final int BROADCAST_RATE = 70;

  public static void startServer() {
    log.info("Starting server on port {}", PORT);

    initPersistence();

    // load default worlds from worlds.yml and init instances
    List<WorldConnector> worlds = loadWorlds();
    initInstances(worlds);

    registerShutdownHook(worlds);

    Server server = new TcpServer(PORT);
    try {
      server.start();
    } catch (IOException ex) {
      log.error("Server failed: {}", ex.getMessage());
    }
  }

  private static void initPersistence() {
    DatabaseBuilder.createDatabase(ConnectionProvider.DEFAULT_DB_PATH, Path.of("data/snapshots"));

    // Hook: persist player stats on every disconnect
    LifecycleHooks.setOnPlayerLeave((playerName, worldName) -> {
      try {
        PersistenceContext.players().upsert(playerName);
        PersistenceContext.players().recordSessionEnd(playerName);
        log.debug("[Persistence] Recorded session end for player '{}' in '{}'.", playerName,
            worldName);
      } catch (Exception e) {
        log.error("[Persistence] Failed to persist disconnect for '{}': {}", playerName,
            e.getMessage(), e);
      }
    });

    log.info("[ServerBootstrap] Persistence layer initialised.");
  }

  private static List<WorldConnector> loadWorlds() {
    WorldConverter converter = new WorldConverter();
    List<WorldConnector> worlds = new WorldConfigReader().readAll().stream()
        .map(converter::toModelObject).toList();

    // Register world metadata and restore terrain for grid worlds
    for (WorldConnector connector : worlds) {
      registerWorldMetadata(connector);
      restoreTerrainIfGridWorld(connector);
    }

    return worlds;
  }

  /**
   * Saves world metadata to the database the first time a world is seen.
   * Subsequent startups are idempotent (row already exists).
   */
  private static void registerWorldMetadata(WorldConnector connector) {
    try {
      // Determine depth: 0 for 2-D grid worlds, actual depth for 3-D open worlds
      int depth = 0; // grid worlds are 2D

      WorldRecord record = new WorldRecord(connector.getWorldName(), connector.getWorldType(), 0, 0,
          depth,   // dimensions not critical in metadata; world config is the source of truth
          Instant.now());
      PersistenceContext.worlds().registerIfAbsent(record);
    } catch (Exception e) {
      log.warn("[ServerBootstrap] Could not register world metadata for '{}': {}",
          connector.getWorldName(), e.getMessage());
    }
  }

  /**
   * For grid-world connectors: if a terrain snapshot exists on disk, replace the freshly
   * generated terrain with the saved one so the map is stable across restarts.
   */
  private static void restoreTerrainIfGridWorld(WorldConnector connector) {
    if (!(connector instanceof GridWorldConnector gridConnector)) {
      return; // open worlds / traffic sims don't have tile terrain to restore
    }

    String worldName = connector.getWorldName();
    PersistenceContext.terrain().load(worldName).ifPresentOrElse(savedTerrain -> {
      gridConnector.restoreTerrain(savedTerrain);
      log.info("[ServerBootstrap] Restored terrain for '{}' ({} tiles).", worldName,
          savedTerrain.size());
    }, () -> {
      // First startup: save the freshly generated terrain so future restarts are stable
      var currentTerrain = ((GridWorldState) gridConnector.getState()).terrain();
      PersistenceContext.terrain().save(worldName, currentTerrain);
      log.info("[ServerBootstrap] Saved initial terrain for '{}'.", worldName);
    });
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
          new ServerInstance(world, sessionManager, clock, new SharedBroadcaster(),
              Settings.multiplayer(BROADCAST_RATE)));
    }
  }

  /**
   * Registers a JVM shutdown hook. Graceful save of final terrain snapshots for all grid
   * worlds and drops db. This runs on Ctrl-C or a normal JVM exit.
   */
  private static void registerShutdownHook(List<WorldConnector> worlds) {
    Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
      log.info("[ServerBootstrap] Shutdown hook: saving terrain snapshots...");
      for (WorldConnector connector : worlds) {
        if (connector instanceof GridWorldConnector gridConnector) {
          try {
            var terrain = gridConnector.getState().terrain();
            PersistenceContext.terrain().save(connector.getWorldName(), terrain);
            log.info("[ServerBootstrap] Saved terrain for '{}'.", connector.getWorldName());
          } catch (Exception e) {
            log.error("[ServerBootstrap] Failed to save terrain for '{}': {}",
                connector.getWorldName(), e.getMessage(), e);
          }
        }
      }
      DatabaseBuilder.dropDatabase();
    }));
  }
}
