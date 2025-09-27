package com.ntros;


import static com.ntros.ServerTestHelper.stopServerWhen;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ntros.event.broadcaster.BroadcastToAll;
import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.sessionmanager.ClientSessionManager;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.instance.InstanceRegistry;
import com.ntros.instance.ins.Instance;
import com.ntros.instance.ins.WorldInstance;
import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.model.entity.config.access.Visibility;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.state.solid.GridWorldState;
import com.ntros.server.TcpServer;
import com.ntros.ticker.Ticker;
import com.ntros.ticker.WorldTicker;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class ServerBootstrapTest {

  private static final int PORT = 5555;
  private static final int TICK_RATE = 100;
  private final SessionManager sessionManager = new ClientSessionManager();

  private final GridWorldConnector DEFAULT_WORLD = createWorldConnector("arena-x", 3, 3);
  private final Ticker serverTicker = new WorldTicker(TICK_RATE);
  private final InstanceConfig instanceConfig = createInstanceConfig(
      100, false, Visibility.PUBLIC, true);
  private final Instance instance = createInstance(DEFAULT_WORLD, sessionManager, serverTicker,
      new BroadcastToAll(), instanceConfig);

  private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
  private TcpServer server;

  @BeforeAll
  static void awaitDefaults() {
    Awaitility.setDefaultPollInterval(Duration.ofMillis(25));
    Awaitility.setDefaultTimeout(Duration.ofSeconds(2));
  }

  @BeforeEach
  void setUp() {
    // register the world instance(state + engine) with the tick server
    InstanceRegistry.registerInstance(instance);

    server = new TcpServer(PORT);
    // Start the server in a background thread.
    ServerTestHelper.startServer(server, serverExecutor, PORT);
  }

  @AfterEach
  public void tearDown() throws IOException {
    instance.reset();
    IdSequenceGenerator.getInstance().resetAll();
    server.stop();
  }

  @Test
  void singleConnection_sendJoinCommand_immediateJoinSuccess() throws Exception {
    // autoStartOnPlayerJoin = true -> server will immediately start ticking
    try (TestClient testClient = new TestClient("localhost", PORT)) {
      String clientName = "client-1";

      // join server
      String actualJoinResponse = testClient.join(clientName, DEFAULT_WORLD.getWorldName(), 2);
      String expectedJoinResponse = "WELCOME " + clientName;
      // Verify success join
      log.info("[TEST]: Received JOIN response from server: {}", actualJoinResponse);
      assertEquals(expectedJoinResponse, actualJoinResponse,
          "Unexpected response from server for " + clientName);
    }
    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = DEFAULT_WORLD.getCurrentEntities();
    log.info("Entities in world: {}", entities);
    assertEquals(0, entities.size());
  }

  @Test
  void singleConnection_sendMoveCommand_moveInTheWorldSuccess() throws Exception {
    try (TestClient testClient = new TestClient("localhost", PORT)) {
      String clientName = "client-1";

      // join server
      String actualJoinResponse = testClient.join(clientName, DEFAULT_WORLD.getWorldName(), 2);
      String expectedJoinResponse = "WELCOME " + clientName;
      // Verify success join
      log.info("[SingleCon_MoveCommand]: Received JOIN response from server: {}",
          actualJoinResponse);
      assertEquals(expectedJoinResponse, actualJoinResponse,
          "Unexpected response from server for " + clientName);

      // send move command to server
      log.info("[SingleCon_MoveCommand]: Sending MOVE request to server...");

      String actualMoveResponse = testClient.move(clientName, "UP", 2);
      // ticker will constantly stream the state, ack command is never sent or is lost between state broadcasts
      log.info("[SingleCon_MoveCommand]: Received MOVE response from server: {}",
          actualMoveResponse);
      assertTrue(actualMoveResponse.contains("STATE {"));
    }
    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = DEFAULT_WORLD.getCurrentEntities();
    log.info("[SingleCon_MoveCommand]: Entities in world: {}", entities);
    assertEquals(0, entities.size());
  }

  @Test
  void multipleClients_sendJoinCommand_welcomeMessageResponse() throws Exception {
    int clientCount = 3;
    List<TestClient> clients = new ArrayList<>();
    try {
      for (int i = 0; i < clientCount; i++) {
        String clientName = "client-" + i;
        TestClient client = new TestClient("localhost", PORT);
        clients.add(client);
        String actualJoinResponse = client.join(clientName, DEFAULT_WORLD.getWorldName(), 2);
        String expectedJoinResponse = "WELCOME " + clientName;
        assertEquals(expectedJoinResponse, actualJoinResponse);
      }
    } finally {
      // TestClient is auto-closable, but with simulating multiple clients, close them explicitly
      for (TestClient client : clients) {
        client.close();
      }
    }
    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = DEFAULT_WORLD.getCurrentEntities();
    log.info("[MultiConn_JoinCommand]: Entities in world: {}", entities);
    assertEquals(0, entities.size());
  }

  @Test
  void multipleClients_sendMoveCommand_receiveWorldStateResponse() throws Exception {
    int clientCount = 3;
    List<TestClient> clients = new ArrayList<>();
    try {
      for (int i = 0; i < clientCount; i++) {
        String clientName = "client-" + i;
        TestClient client = new TestClient("localhost", PORT);
        clients.add(client);
        String actualJoinResponse = client.join(clientName, DEFAULT_WORLD.getWorldName(), 100);
        String expectedJoinResponse = "WELCOME " + clientName;
        assertEquals(expectedJoinResponse, actualJoinResponse);

        // send move command to server
        log.info("[MultiConn_MoveCommand]: Sending MOVE request to server...");
        String actualMoveResponse = client.move(clientName, "UP", 5);
        String expectedMoveResponse = "STATE {";
        log.info("[MultiConn_MoveCommand]: Received MOVE response from server: {}",
            actualMoveResponse);
        assertTrue(actualMoveResponse.contains(expectedMoveResponse));
      }
    } finally {
      // TestClient is auto-closable, but with simulating multiple clients, close them explicitly
      for (TestClient client : clients) {
        client.close();
      }
    }
    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = DEFAULT_WORLD.getCurrentEntities();
    log.info("[MultiConn_MoveCommand]: Entities in world: {}", entities);
    assertEquals(0, entities.size());
  }

  @Test
  void multipleClients_joinDifferentWorlds_welcomeMessageResponse() throws Exception {
    // register second world
    var secondWorld = createWorldConnector("arena-y", 3, 3);

    var secondInstance = createMultiplayerInstance(secondWorld,
        new InstanceConfig(100, false, Visibility.PUBLIC, true));
    InstanceRegistry.registerInstance(secondInstance);

    try (TestClient c1 = new TestClient("localhost", PORT);
        TestClient c2 = new TestClient("localhost", PORT)) {

      String w1 = DEFAULT_WORLD.getWorldName(); // e.g., "arena-x"
      String w2 = "arena-y";                    // make sure you registered a second world

      String r1 = c1.join("client-1", w1, 2); // 2s is plenty now
      String r2 = c2.join("client-2", w2, 2);

      assertEquals("WELCOME client-1", r1);
      assertEquals("WELCOME client-2", r2);
    }

    stopServerWhen(List.of(instance, secondInstance), server, serverExecutor);
    assertEquals(0, DEFAULT_WORLD.getCurrentEntities().size());
  }

  private GridWorldConnector createWorldConnector(String worldName, int width, int height) {
    return new GridWorldConnector(new GridWorldState(worldName, width, height),
        new GridWorldEngine(),
        new WorldCapabilities(true, true,
            false, true));
  }

  private WorldInstance createInstance(WorldConnector worldConnector, SessionManager sessionManager,
      Ticker ticker, Broadcaster broadcaster, InstanceConfig config) {
    return new WorldInstance(worldConnector, sessionManager, ticker, broadcaster, config);
  }

  private WorldInstance createMultiplayerInstance(WorldConnector worldConnector,
      InstanceConfig config) {
    return new WorldInstance(worldConnector, new ClientSessionManager(), new WorldTicker(TICK_RATE),
        new BroadcastToAll(), config);
  }

  private InstanceConfig createInstanceConfig(int maxPlayers, boolean requiresOrchestrator,
      Visibility visibility, boolean autoStartOnPlayerJoin) {
    return new InstanceConfig(maxPlayers, requiresOrchestrator, visibility, autoStartOnPlayerJoin);
  }

}