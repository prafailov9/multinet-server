package com.ntros;


import com.ntros.config.converter.WorldConverter;
import com.ntros.broadcast.SharedBroadcaster;
import com.ntros.lifecycle.sessionmanager.ClientSessionManager;
import com.ntros.lifecycle.sessionmanager.SessionManager;
import com.ntros.lifecycle.LifecycleHooks;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.clock.FixedRateClock;
import com.ntros.lifecycle.clock.PacedRateClock;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.instance.ServerInstance;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.persistence.db.ConnectionProvider;
import com.ntros.persistence.db.DatabaseBuilder;
import com.ntros.persistence.db.PersistenceContext;
import com.ntros.persistence.model.WorldRecord;
import com.ntros.server.Server;
import com.ntros.server.TcpServer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

    // Create db
    initPersistence();

    // Populates initial users in db.
    // Convenience for skipping REG/AUTH flow during dev
    ClientSeedLoader.seedIfEmpty();

    // seed + load worlds from DB, then init instances
    List<WorldConnector> worlds = loadWorlds();
    initInstances(worlds);

    // on server-stop - run cleanup and save world state
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

  /**
   * Seeds default worlds into the DB (first startup only), then loads all world records and
   * converts each to a live {@link WorldConnector}.
   *
   * <p>GoL worlds always start blank and are seeded at runtime via ORCHESTRATE commands —
   * terrain snapshots are skipped for them.
   */
  private static List<WorldConnector> loadWorlds() {
    WorldSeedLoader.seedIfEmpty();

    WorldConverter converter = new WorldConverter();
    List<WorldConnector> worlds = new ArrayList<>();
    for (WorldRecord record : PersistenceContext.worlds().findAll()) {
      WorldConnector connector = converter.toModelObject(record);
      if (!"GOL".equalsIgnoreCase(record.engineType())) {
        restoreTerrainIfGridWorld(connector);
      }
      worlds.add(connector);
    }
    return worlds;
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
      var currentTerrain = (gridConnector.getState()).terrain();
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

      // Terrain snapshots are only meaningful for GRID worlds — GoL worlds always restart blank.
      Set<String> golNames = PersistenceContext.worlds().findAll().stream()
          .filter(r -> "GOL".equalsIgnoreCase(r.engineType()))
          .map(WorldRecord::name)
          .collect(Collectors.toSet());

      for (WorldConnector connector : worlds) {
        if (connector instanceof GridWorldConnector gridConnector
            && !golNames.contains(connector.getWorldName())) {
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
