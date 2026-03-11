package com.ntros;


import static com.ntros.ServerTestHelper.startServer;
import static com.ntros.ServerTestHelper.stopServerWhen;
import static com.ntros.TestClient.ServerCmd.REG_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntros.TestClient.ServerCmd;
import com.ntros.TestClient.ServerMessage;
import com.ntros.event.broadcaster.SessionsBroadcaster;
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
import com.ntros.persistence.PersistenceContext;
import com.ntros.persistence.db.ConnectionProvider;
import com.ntros.persistence.repository.impl.JsonTerrainSnapshotRepository;
import com.ntros.persistence.repository.impl.SqliteClientRepository;
import com.ntros.persistence.repository.impl.SqlitePlayerRepository;
import com.ntros.persistence.repository.impl.SqliteWorldRepository;
import com.ntros.server.TcpServer;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.clock.PacedRateClock;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

  private static Path terrainDir;

  private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
  private TcpServer server;
  private Instance instance;
  private WorldConnector defaultWorld;

  @BeforeAll
  static void classSetUp() throws IOException {
    Awaitility.setDefaultPollInterval(Duration.ofMillis(25));
    Awaitility.setDefaultTimeout(Duration.ofSeconds(2));
    terrainDir = Files.createTempDirectory("test-terrain-");
  }

  @BeforeEach
  void setUp() {
    // Initialise an in-memory SQLite database so commands that touch PersistenceContext work.
    ConnectionProvider.initialize(":memory:");
    PersistenceContext.init(new SqliteClientRepository(),
        new SqlitePlayerRepository(),
        new SqliteWorldRepository(),
        new JsonTerrainSnapshotRepository(terrainDir)
    );

    // register the world instance(state + engine) with the tick server
    defaultWorld = createWorldConnector("arena-x", 3, 3);
    instance = createSingleplayerInstance(defaultWorld, new ClientSessionManager(),
        new PacedRateClock(TICK_RATE), new SessionsBroadcaster(),
        Settings.multiplayer(BROADCAST_RATE)
    );
    Instances.registerInstance(instance);
    server = new TcpServer(PORT);
  }

  @AfterEach
  void tearDown() throws IOException {
    IdSequenceGenerator.getInstance().resetAll();
    Instances.clear();
    // Safety net: ensure the server is always stopped, even if the test body threw
    // before reaching stopServerWhen() (e.g. ConditionTimeoutException).
    // TcpServer.stop() is idempotent — safe to call if already stopped.
    if (server != null) {
      server.stop();
    }
    // Tear down persistence so the next test can call PersistenceContext.init() again.
    PersistenceContext.reset();
    ConnectionProvider.close();
  }

  @Test
  public void singleConn_regInSystem_Success() throws IOException {
    startServer(server, serverExecutor, PORT);
    try (TestClient testClient = new TestClient("localhost", PORT)) {
      String clientName = "client-1";
      String pass = "123";
      ServerMessage regResponse = testClient.reg(clientName, pass, 2);

      assertEquals(REG_SUCCESS, regResponse.type());
      // Session ID is a positive long; avoid asserting exact value since the startServer
      // port-probe connection consumes at least one ID before the test client connects.
      assertTrue(Long.parseLong(regResponse.args().getFirst()) > 0, "sessionId must be positive");
      assertEquals("client-1", regResponse.args().getLast());

    }

    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = defaultWorld.getCurrentEntities();
    // verify cleanup success
    assertEquals(0, entities.size());
  }

  @Test
  void singleConnection_sendJoinCommand_immediateJoinSuccess() throws Exception {
    // autoStartOnPlayerJoin = true -> server will immediately start ticking
    startServer(server, serverExecutor, PORT);
    try (TestClient testClient = new TestClient("localhost", PORT)) {
      String clientName = "client-1";

      // join server
      ServerMessage actualJoinResponse = testClient.join(clientName, defaultWorld.getWorldName(),
          2);
      // Verify success join
      log.info("[TEST]: Received JOIN response from server: {}", actualJoinResponse);
      assertEquals(ServerCmd.WELCOME, actualJoinResponse.type());
      assertEquals("client-1", actualJoinResponse.args().getFirst());
    }
    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = defaultWorld.getCurrentEntities();
    log.info("Entities in world: {}", entities);
    assertEquals(0, entities.size());
  }

  @Test
  void singleConnection_sendMoveCommand_moveInTheWorldSuccess() throws Exception {
    startServer(server, serverExecutor, PORT);
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
      // GridSnapshot serialises as {"tiles":{...},"entities":{...}} — no "data" wrapper
      assertTrue(parsedJson.has("entities"));
      assertTrue(parsedJson.get("entities").has("client-1"));

    }
    // stop server
    stopServerWhen(instance, server, serverExecutor);

    List<Entity> entities = defaultWorld.getCurrentEntities();
    log.info("[SingleCon_MoveCommand]: Entities in world: {}", entities);
    assertEquals(0, entities.size());
  }

  @Test
  void multipleClients_sendJoinCommand_welcomeMessageResponse() throws Exception {
    startServer(server, serverExecutor, PORT);
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
    startServer(server, serverExecutor, PORT);
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
        // GridSnapshot serialises as {"tiles":{...},"entities":{...}} — no "data" wrapper
        assertTrue(parsedJson.has("entities"));
        assertTrue(parsedJson.get("entities").has("client-0"));

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

    var secondWorld = createWorldConnector("arena-y", 3, 3);

    var secondInstance = createMultiplayerInstance(
        secondWorld,
        Settings.multiplayer(BROADCAST_RATE));

    Instances.registerInstance(secondInstance);

    // NOW start server
    server = new TcpServer(PORT);
    startServer(server, serverExecutor, PORT);

    try (TestClient c1 = new TestClient("localhost", PORT);
        TestClient c2 = new TestClient("localhost", PORT)) {

      String w1 = defaultWorld.getWorldName();
      String w2 = "arena-y";

      ServerMessage response1 = c1.join("client-1", w1, 2);
      ServerMessage response2 = c2.join("client-2", w2, 2);

      assertEquals(ServerCmd.WELCOME, response1.type());
      assertEquals("client-1", response1.args().getFirst());

      assertEquals(ServerCmd.WELCOME, response2.type());
      assertEquals("client-2", response2.args().getFirst());
    }

    stopServerWhen(List.of(instance, secondInstance), server, serverExecutor);
  }

  private GridWorldConnector createWorldConnector(String worldName, int width, int height) {
    return new GridWorldConnector(new GridWorldState(worldName, width, height, SEEDED),
        new GridWorldEngine(),
        new WorldCapabilities(true, true,
            false, true));
  }

  private ServerInstance createSingleplayerInstance(WorldConnector worldConnector,
      SessionManager sessionManager,
      Clock clock, Broadcaster broadcaster, Settings settings) {
    return new ServerInstance(worldConnector, sessionManager, clock, broadcaster, settings);
  }

  private ServerInstance createMultiplayerInstance(WorldConnector worldConnector,
      Settings settings) {
    return new ServerInstance(worldConnector, new ClientSessionManager(),
        new PacedRateClock(TICK_RATE),
        new SessionsBroadcaster(), settings);
  }


}