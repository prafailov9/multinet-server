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
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.entity.config.access.InstanceSettings;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WaTorConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.d2.grid.fallingsand.FallingSandEngine;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.state.grid.FallingSandState;
import com.ntros.model.world.wator.WaTorEngineImpl;
import com.ntros.model.world.wator.WaTorWorld;
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
  private static final int GOL_BROADCAST_RATE = 10;
  private static final int SAND_BROADCAST_RATE = 120;
  private static final int WATOR_BROADCAST_RATE = 10;

  private record SandWorldDef(String name, int w, int h, boolean randomSeed) {

  }

  private static final List<SandWorldDef> SAND_WORLDS = List.of(
      new SandWorldDef("falling-sand", 128, 128, false),
      new SandWorldDef("falling-sand-small", 32, 32, true),
      new SandWorldDef("falling-sand-mid", 64, 64, true)
  );

  // Wa-Tor initial population
  private static final int WATOR_INITIAL_PREDATORS = 40;
  private static final int WATOR_INITIAL_PREY = 120;
  private static final int WATOR_INITIAL_PLANTS = 180;

  public static void startServer() {
    log.info("Starting server on port {}", PORT);

    // Create db
    initPersistence();

    // Populates initial users in db.
    // Convenience for skipping REG/AUTH flow during dev
    ClientSeedLoader.seedIfEmpty();

    // seed + load grid/GoL worlds from DB, then init instances
    List<WorldConnector> worlds = loadWorlds();
    initInstances(worlds);

    // Bootstrap autonomous simulations (not DB-backed — seeded programmatically)
    bootstrapFallingSandWorlds();
    bootstrapWaTorWorlds();

    // on server-stop - run cleanup and save world state
//    registerShutdownHook(worlds);

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

      // restore-terrain is brittle currently. Needs rework
      // TODO: restore terrain only if player requests that specific world again(lazy-loading)
//      if (!"GOL".equalsIgnoreCase(record.engineType())) {
//        restoreTerrainIfGridWorld(connector);
//      }
      worlds.add(connector);
    }
    return worlds;
  }

//  /**
//   * For grid-world connectors: if a terrain snapshot exists on disk, replace the freshly
//   * generated terrain with the saved one so the map is stable across restarts.
//   */
//  private static void restoreTerrainIfGridWorld(WorldConnector connector) {
//    if (!(connector instanceof GridWorldConnector gridConnector)) {
//      return; // open worlds / traffic sims don't have tile terrain to restore
//    }
//    String worldName = connector.getWorldName();
//    log.info("[ServerBootstrap] Started restoring terrain for '{}'.", worldName);
//    PersistenceContext.terrain().load(worldName).ifPresentOrElse(savedTerrain -> {
//      gridConnector.restoreTerrain(savedTerrain);
//      log.info("[ServerBootstrap] Restored terrain for '{}' ({} tiles).", worldName,
//          savedTerrain.size());
//    }, () -> {
//      // First startup: save the freshly generated terrain so future restarts are stable
//      var currentTerrain = (gridConnector.getState()).terrain();
//      PersistenceContext.terrain().save(worldName, currentTerrain);
//      log.info("[ServerBootstrap] Saved initial terrain for '{}'.", worldName);
//    });
//  }

  private static void initInstances(List<WorldConnector> worlds) {
    for (WorldConnector world : worlds) {
      SessionManager sessionManager = new ClientSessionManager();
      // build clock based on worlds cap:
      Clock clock = new PacedRateClock(TICK_RATE); // default if cap cfg missing
      // if singleplayer: fixed clock
      if (!world.getCapabilities().supportsPlayers()) {
        clock = new FixedRateClock(TICK_RATE);
      }

      // orchestrated worlds (GoL etc.) require an ORCHESTRATE command to seed them before ticking
      InstanceSettings instanceSettings = world.getCapabilities().supportsOrchestrator()
          ? InstanceSettings.multiplayerOrchestrator(GOL_BROADCAST_RATE)
          : InstanceSettings.multiplayer(BROADCAST_RATE);

      Instances.registerInstance(
          new ServerInstance(world, sessionManager, clock, new SharedBroadcaster(),
              instanceSettings));
    }
  }

  /**
   * Bootstraps all Falling Sand worlds. Skips any world already registered from DB.
   * Randomly-seeded worlds are pre-filled and their simulation clock is started immediately.
   */
  private static void bootstrapFallingSandWorlds() {
    for (SandWorldDef def : SAND_WORLDS) {
      if (Instances.getInstance(def.name()) != null) {
        log.info("[ServerBootstrap] Sand world '{}' already registered — skipping.", def.name());
        continue;
      }
      FallingSandState state = new FallingSandState(def.name(), def.w(), def.h());
      FallingSandEngine engine = new FallingSandEngine();
      WorldCapabilities caps = new WorldCapabilities(true, true, false, true);
      GridWorldConnector connector = new GridWorldConnector(state, engine, caps);

      SessionManager sessionManager = new ClientSessionManager();
      Clock clock = new FixedRateClock(TICK_RATE);
      InstanceSettings instanceSettings = InstanceSettings.multiplayerOrchestrator(SAND_BROADCAST_RATE);
      ServerInstance instance = new ServerInstance(connector, sessionManager, clock,
          new SharedBroadcaster(), instanceSettings);
      Instances.registerInstance(instance);

      if (def.randomSeed()) {
        // Seeding triggers ensureBuffers + starts the clock (instance goes live)
        instance.orchestrateAsync(OrchestrateRequest.randomSeed(0f)).join();
      }
      log.info("[ServerBootstrap] Sand world '{}' registered ({}×{}, random={}).",
          def.name(), def.w(), def.h(), def.randomSeed());
    }
  }

  /**
   * Creates and starts Wa-Tor worlds with a seeded initial population.
   *
   * <p>Wa-Tor worlds are not persisted in the DB — they are always bootstrapped fresh on
   * server start.  The simulation runs autonomously from boot; observer sessions may join
   * and leave without affecting the clock.
   */
  private static void bootstrapWaTorWorlds() {
    String[] watorNames = {"wator"};
    for (String name : watorNames) {
      WaTorWorld world = new WaTorWorld(name);
      WaTorEngineImpl engine = new WaTorEngineImpl();

      // Seed initial population
      for (int i = 0; i < WATOR_INITIAL_PREDATORS; i++) {
        world.spawnPredator();
      }
      for (int i = 0; i < WATOR_INITIAL_PREY; i++) {
        world.spawnPrey();
      }
      for (int i = 0; i < WATOR_INITIAL_PLANTS; i++) {
        float x = (float) (Math.random() * com.ntros.model.world.wator.WaTorWorldState.WIDTH);
        float y = (float) (Math.random() * com.ntros.model.world.wator.WaTorWorldState.HEIGHT);
        world.spawnPlant(x, y);
      }

      WorldCapabilities caps = new WorldCapabilities(true, false, true, false);
      WaTorConnector connector = new WaTorConnector(world, engine, caps);

      SessionManager sessionManager = new ClientSessionManager();
      Clock clock = new FixedRateClock(TICK_RATE);
      InstanceSettings instanceSettings = InstanceSettings.autonomousSimulation(WATOR_BROADCAST_RATE);
      ServerInstance instance = new ServerInstance(connector, sessionManager, clock,
          new SharedBroadcaster(), instanceSettings);

      Instances.registerInstance(instance);
//      instance.start();   // autonomous — runs immediately, independent of observer count
      log.info("[ServerBootstrap] Wa-Tor world '{}' started ({} predators, {} prey, {} plants).",
          name, WATOR_INITIAL_PREDATORS, WATOR_INITIAL_PREY, WATOR_INITIAL_PLANTS);
    }
  }

  /**
   * Registers a JVM shutdown hook. Graceful save of final terrain snapshots for all grid
   * worlds and drops db. This runs on Ctrl-C or a normal JVM exit.
   */
//  private static void registerShutdownHook(List<WorldConnector> worlds) {
//    Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
//      log.info("[ServerBootstrap] Shutdown hook: saving terrain snapshots...");
//
//      // Terrain snapshots are only meaningful for GRID worlds — GoL worlds always restart blank.
//      Set<String> golNames = PersistenceContext.worlds().findAll().stream()
//          .filter(r -> "GOL".equalsIgnoreCase(r.engineType()))
//          .map(WorldRecord::name)
//          .collect(Collectors.toSet());
//
//      for (WorldConnector connector : worlds) {
//        if (connector instanceof GridWorldConnector gridConnector
//            && !golNames.contains(connector.getWorldName())) {
//          try {
//            var terrain = gridConnector.getState().terrain();
//            PersistenceContext.terrain().save(connector.getWorldName(), terrain);
//            log.info("[ServerBootstrap] Saved terrain for '{}'.", connector.getWorldName());
//          } catch (Exception e) {
//            log.error("[ServerBootstrap] Failed to save terrain for '{}': {}",
//                connector.getWorldName(), e.getMessage(), e);
//          }
//        }
//      }
//      DatabaseBuilder.dropDatabase();
//    }));
//  }
}
