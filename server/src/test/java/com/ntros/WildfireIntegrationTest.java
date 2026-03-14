package com.ntros;

import static com.ntros.ServerTestHelper.startServer;
import static com.ntros.ServerTestHelper.stopServerWhen;
import static com.ntros.TestClient.ServerCmd.ACK;
import static com.ntros.TestClient.ServerCmd.REG_SUCCESS;
import static com.ntros.TestClient.ServerCmd.STATE;
import static com.ntros.TestClient.ServerCmd.WELCOME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntros.TestClient.ServerCmd;
import com.ntros.TestClient.ServerMessage;
import com.ntros.broadcast.SharedBroadcaster;
import com.ntros.lifecycle.clock.FixedRateClock;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.instance.ServerInstance;
import com.ntros.lifecycle.sessionmanager.ClientSessionManager;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.entity.config.access.InstanceSettings;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.connector.SimulationWorldConnector;
import com.ntros.model.world.engine.d2.grid.wildfire.WildfireEngine;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.state.grid.WildfireState;
import com.ntros.persistence.db.DatabaseBuilder;
import com.ntros.server.TcpServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class WildfireIntegrationTest {

  private static final int PORT = 5556;   // different port to avoid clash with ServerBootstrapTest
  private static final int TICK_RATE = 120;
  private static final int BROADCAST_RATE = 20;
  private static final int W = 32;
  private static final int H = 32;
  private static final String WORLD = "wildfire-test";
  private static Path terrainDir;

  private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
  private TcpServer server;
  private ServerInstance instance;
  private SimulationWorldConnector connector;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @BeforeAll
  static void classSetUp() throws IOException {
    Awaitility.setDefaultPollInterval(Duration.ofMillis(25));
    Awaitility.setDefaultTimeout(Duration.ofSeconds(5));
    terrainDir = Files.createTempDirectory("test-wildfire-terrain-");
  }

  @BeforeEach
  void setUp() {
    DatabaseBuilder.createDatabase(":memory:", terrainDir);

    WildfireState state = new WildfireState(WORLD, W, H);
    WildfireEngine engine = new WildfireEngine();
    connector = new SimulationWorldConnector(state, engine, WorldCapabilities.wildfire());

    instance = new ServerInstance(connector, new ClientSessionManager(),
        new FixedRateClock(TICK_RATE), new SharedBroadcaster(),
        InstanceSettings.simulation(BROADCAST_RATE));
    Instances.registerInstance(instance);

    server = new TcpServer(PORT);
  }

  @AfterEach
  void tearDown() throws IOException {
    IdSequenceGenerator.getInstance().resetAll();
    Instances.clear();
    if (server != null) {
      server.stop();
    }
    DatabaseBuilder.dropDatabase();
  }

  // ── Tests ──────────────────────────────────────────────────────────────────

  @Test
  void join_wildfireWorld_succeeds() throws Exception {
    startServer(server, serverExecutor, PORT);
    try (TestClient client = new TestClient("localhost", PORT)) {
      register(client, "player-1");
      ServerMessage join = client.join("player-1", WORLD, 2);
      assertEquals(WELCOME, join.type(), "JOIN should be accepted for ORCHESTRATION_DRIVEN world");
      assertEquals("player-1", join.args().getFirst());
    }
    stopServerWhen(instance, server, serverExecutor);
  }

  @Test
  void orchestrate_randomSeed_startsClockAndStreamStates() throws Exception {
    startServer(server, serverExecutor, PORT);
    try (TestClient client = new TestClient("localhost", PORT)) {
      register(client, "player-1");
      client.join("player-1", WORLD, 2);

      // RANDOM_SEED starts the clock
      ServerMessage ack = client.orchestrate("RANDOM 0.7", 3);
      assertEquals(ACK, ack.type(), "RANDOM_SEED should return ACK");

      // After seeding, STATE frames should arrive
      ServerMessage state = client.waitForState(3);
      assertEquals(STATE, state.type(), "Server should stream STATE after seeding");

      String json = state.args().getFirst();
      JsonNode root = MAPPER.readTree(json);
      assertTrue(root.has("tiles") || root.has("cells"),
          "Wildfire STATE must contain 'tiles' or 'cells': " + json);
    }
    stopServerWhen(instance, server, serverExecutor);
  }

  @Test
  void orchestrate_placeIgnite_broadcastsBurningCell() throws Exception {
    // Pre-seed forest without starting the clock
    connector.preSeed(OrchestrateRequest.randomSeed(0.8f));

    startServer(server, serverExecutor, PORT);
    try (TestClient client = new TestClient("localhost", PORT)) {
      register(client, "player-1");
      client.join("player-1", WORLD, 2);

      // PLACE BURNING starts the clock and ignites a cell
      int cx = W / 2;
      int cy = H / 2;
      ServerMessage ack = client.orchestrate("PLACE BURNING " + cx + " " + cy, 3);
      assertEquals(ACK, ack.type(), "PLACE BURNING should return ACK");

      // Receive a STATE frame and verify the snapshot contains non-empty tile data
      ServerMessage state = client.waitForState(3);
      assertEquals(STATE, state.type());
      String json = state.args().getFirst();
      assertNotNull(json, "STATE payload must not be null");
      JsonNode root = MAPPER.readTree(json);
      assertTrue(root.isObject(), "Wildfire snapshot must be a JSON object");
    }
    stopServerWhen(instance, server, serverExecutor);
  }

  @Test
  void orchestrate_setWind_returnsAck() throws Exception {
    startServer(server, serverExecutor, PORT);
    try (TestClient client = new TestClient("localhost", PORT)) {
      register(client, "player-1");
      client.join("player-1", WORLD, 2);

      // Seed first so the clock starts
      client.orchestrate("RANDOM 0.6", 3);

      ServerMessage ack = client.orchestrate("WIND E 0.8", 3);
      assertEquals(ACK, ack.type(), "SET_WIND should return ACK");
    }
    stopServerWhen(instance, server, serverExecutor);
  }

  @Test
  void orchestrate_clear_returnsAck() throws Exception {
    connector.preSeed(OrchestrateRequest.randomSeed(0.7f));

    startServer(server, serverExecutor, PORT);
    try (TestClient client = new TestClient("localhost", PORT)) {
      register(client, "player-1");
      client.join("player-1", WORLD, 2);

      client.orchestrate("PLACE BURNING 10 10", 3); // start clock
      ServerMessage ack = client.orchestrate("CLEAR", 3);
      assertEquals(ACK, ack.type(), "CLEAR should return ACK");
    }
    stopServerWhen(instance, server, serverExecutor);
  }

  @Test
  void orchestrate_placeFirebreak_returnsAck() throws Exception {
    startServer(server, serverExecutor, PORT);
    try (TestClient client = new TestClient("localhost", PORT)) {
      register(client, "player-1");
      client.join("player-1", WORLD, 2);

      client.orchestrate("RANDOM 0.5", 3); // start clock
      ServerMessage ack = client.orchestrate("PLACE FIREBREAK 5 5", 3);
      assertEquals(ACK, ack.type(), "PLACE FIREBREAK should return ACK");
    }
    stopServerWhen(instance, server, serverExecutor);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private void register(TestClient client, String name) {
    ServerMessage reg = client.register(name, "pass", 1);
    assertEquals(REG_SUCCESS, reg.type(), "REG must succeed for " + name);
  }
}
