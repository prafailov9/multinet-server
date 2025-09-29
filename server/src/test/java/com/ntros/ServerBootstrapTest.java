package com.ntros;


import static com.ntros.ServerTestHelper.stopServerWhen;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntros.TestClient.ServerCmd;
import com.ntros.TestClient.ServerMessage;
import com.ntros.event.broadcaster.BroadcastToAll;
import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.sessionmanager.ClientSessionManager;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.ServerInstance;
import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.state.solid.GridWorldState;
import com.ntros.server.TcpServer;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.clock.PacedRateClock;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
  private static final int BROADCAST_RATE = 21;

  private static final Random SEEDED = new Random(10);

  private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
  private TcpServer server;

  private Instance instance;
  private WorldConnector defaultWorld;

  @BeforeAll
  static void awaitDefaults() {
    Awaitility.setDefaultPollInterval(Duration.ofMillis(25));
    Awaitility.setDefaultTimeout(Duration.ofSeconds(2));
  }

  @BeforeEach
  void setUp() {
    // register the world instance(state + engine) with the tick server

    defaultWorld = createWorldConnector("arena-x", 3, 3);
    instance = createInstance(defaultWorld, new ClientSessionManager(),
        new PacedRateClock(TICK_RATE), new BroadcastToAll(), Settings.multiplayer(BROADCAST_RATE)
    );

    Instances.registerInstance(instance);

    server = new TcpServer(PORT);
    ServerTestHelper.startServer(server, serverExecutor, PORT);
    Instances.registerInstance(instance);

    server = new TcpServer(PORT);
    // Start the server in a background thread.
    ServerTestHelper.startServer(server, serverExecutor, PORT);
  }

  @AfterEach
  public void tearDown() throws IOException {
    instance.reset();
    server.stop();
    serverExecutor.shutdownNow();
    IdSequenceGenerator.getInstance().resetAll();
    Instances.clear();
  }

  @Test
  void singleConnection_sendJoinCommand_immediateJoinSuccess() throws Exception {
    // autoStartOnPlayerJoin = true -> server will immediately start ticking
    try (TestClient testClient = new TestClient("localhost", PORT)) {
      String clientName = "client-1";

      // join server
      ServerMessage actualJoinResponse = testClient.join(clientName, defaultWorld.getWorldName(),
          2);
//      String actualJoinResponse = testClient.join(clientName, defaultWorld.getWorldName(), 2);
      String expectedJoinResponse = "WELCOME " + clientName;
      // Verify success join
      log.info("[TEST]: Received JOIN response from server: {}", actualJoinResponse);
      assertEquals(ServerCmd.WELCOME, actualJoinResponse.type());
      assertEquals("client-1", actualJoinResponse.args().getFirst());
//      assertEquals(expectedJoinResponse, actualJoinResponse,
//          "Unexpected response from server for " + clientName);
    }
    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = defaultWorld.getCurrentEntities();
    log.info("Entities in world: {}", entities);
    assertEquals(0, entities.size());
  }

  @Test
  void singleConnection_sendMoveCommand_moveInTheWorldSuccess() throws Exception {
    try (TestClient testClient = new TestClient("localhost", PORT)) {
      String clientName = "client-1";

      // join server
      ServerMessage actualJoinResponse = testClient.join(clientName, defaultWorld.getWorldName(),
          2);
      // Verify success join
      log.info("[SingleCon_MoveCommand]: Received JOIN response from server: {}",
          actualJoinResponse);

      assertEquals(ServerCmd.WELCOME, actualJoinResponse.type());
      assertEquals("client-1", actualJoinResponse.args().getFirst());

      // send move command to server
      log.info("[SingleCon_MoveCommand]: Sending MOVE request to server...");

      ServerMessage actualMoveResponse = testClient.move(clientName, "UP", 2);
      // ticker will constantly stream the state, ack command is never sent or is lost between state broadcasts
      assertEquals(ServerCmd.STATE, actualMoveResponse.type());

      String json = actualMoveResponse.args().getFirst();
      var parsedJson = new ObjectMapper().readTree(json);
      assertTrue(parsedJson.has("data"));

      var stateJson = parsedJson.get("data");
      assertTrue(stateJson.has("entities"));
      assertTrue(stateJson.get("entities").has("client-1"));

    }
    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = defaultWorld.getCurrentEntities();
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
        ServerMessage actualJoinResponse = client.join(clientName, defaultWorld.getWorldName(), 2);
//        String expectedJoinResponse = "WELCOME " + clientName;
        assertEquals(ServerCmd.WELCOME, actualJoinResponse.type());
        assertEquals(clientName, actualJoinResponse.args().getFirst());
      }
    } finally {
      // TestClient is auto-closable, but with simulating multiple clients, close them explicitly
      for (TestClient client : clients) {
        client.close();
      }
    }
    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = defaultWorld.getCurrentEntities();
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
        ServerMessage actualJoinResponse = client.join(clientName, defaultWorld.getWorldName(),
            100);
        assertEquals(ServerCmd.WELCOME, actualJoinResponse.type());
        assertEquals(clientName, actualJoinResponse.args().getFirst());

        // send move command to server
        log.info("[MultiConn_MoveCommand]: Sending MOVE request to server...");
        ServerMessage actualMoveResponse = client.move(clientName, "UP", 5);
        assertEquals(ServerCmd.STATE, actualMoveResponse.type());

        String json = actualMoveResponse.args().getFirst();
        var parsedJson = new ObjectMapper().readTree(json);
        assertTrue(parsedJson.has("data"));

        var stateJson = parsedJson.get("data");
        assertTrue(stateJson.has("entities"));
        assertTrue(stateJson.get("entities").has("client-0"));

      }
    } finally {
      // TestClient is auto-closable, but with simulating multiple clients, close them explicitly
      for (TestClient client : clients) {
        client.close();
      }
    }
    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = defaultWorld.getCurrentEntities();
    log.info("[MultiConn_MoveCommand]: Entities in world: {}", entities);
    assertEquals(0, entities.size());
  }

  @Test
  void multipleClients_joinDifferentWorlds_welcomeMessageResponse() throws Exception {
    // register second world
    var secondWorld = createWorldConnector("arena-y", 3, 3);

    var secondInstance = createMultiplayerInstance(secondWorld,
        Settings.multiplayer(BROADCAST_RATE));
    Instances.registerInstance(secondInstance);

    try (TestClient c1 = new TestClient("localhost", PORT);
        TestClient c2 = new TestClient("localhost", PORT)) {

      String w1 = defaultWorld.getWorldName(); // e.g., "arena-x"
      String w2 = "arena-y";                    // make sure you registered a second world

      ServerMessage response1 = c1.join("client-1", w1, 2); // 2s is plenty now
      ServerMessage response2 = c2.join("client-2", w2, 2);

      assertEquals(ServerCmd.WELCOME, response1.type());
      assertEquals("client-1", response1.args().getFirst());

      assertEquals(ServerCmd.WELCOME, response2.type());
      assertEquals("client-2", response2.args().getFirst());
    }

    stopServerWhen(List.of(instance, secondInstance), server, serverExecutor);
    assertEquals(0, defaultWorld.getCurrentEntities().size());
  }

  private GridWorldConnector createWorldConnector(String worldName, int width, int height) {
    return new GridWorldConnector(new GridWorldState(worldName, width, height, SEEDED),
        new GridWorldEngine(),
        new WorldCapabilities(true, true,
            false, true));
  }

  private ServerInstance createInstance(WorldConnector worldConnector,
      SessionManager sessionManager,
      Clock clock, Broadcaster broadcaster, Settings policy) {
    return new ServerInstance(worldConnector, sessionManager, clock, broadcaster, policy);
  }

  private ServerInstance createMultiplayerInstance(WorldConnector worldConnector,
      Settings policy) {
    return new ServerInstance(worldConnector, new ClientSessionManager(),
        new PacedRateClock(TICK_RATE),
        new BroadcastToAll(), policy);
  }


}