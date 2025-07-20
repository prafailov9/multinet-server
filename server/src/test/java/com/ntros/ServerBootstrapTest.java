package com.ntros;


import com.ntros.event.bus.SessionEventBus;
import com.ntros.event.listener.ClientSessionEventListener;
import com.ntros.event.listener.ClientSessionManager;
import com.ntros.event.listener.SessionEventListener;
import com.ntros.event.listener.SessionManager;
import com.ntros.model.entity.Entity;
import com.ntros.model.world.WorldDispatcher;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.state.solid.GridWorldState;
import com.ntros.runtime.Instance;
import com.ntros.runtime.WorldInstance;
import com.ntros.server.TcpServer;
import com.ntros.server.scheduler.WorldTickScheduler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.ntros.ServerTestHelper.stopServerWhen;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ServerBootstrapTest {


    private static final int PORT = 5555;
    private static final int TICK_RATE = 1;
    private final GridWorldEngine engine = new GridWorldEngine();
    private final GridWorldConnector world = new GridWorldConnector(new GridWorldState("test-world", 3, 3), engine);
    private TcpServer server;
    private final SessionManager sessionManager = new ClientSessionManager();
    private final Instance instance = new WorldInstance(world, sessionManager);
    private final WorldTickScheduler worldTickScheduler = new WorldTickScheduler(TICK_RATE);
    SessionEventListener clientSessionEventListener = new ClientSessionEventListener(sessionManager, worldTickScheduler);
    private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();


    @BeforeEach
    void setUp() {
        WorldDispatcher.register(world.worldName(), world);
        // register the world instance(state + engine) with the tick server
        worldTickScheduler.register(instance);
        SessionEventBus.get().register(clientSessionEventListener);
        server = new TcpServer(sessionManager);
        // Start the server in a background thread.
        ServerTestHelper.startServer(server, serverExecutor, PORT);
    }

    @AfterEach
    public void tearDown() {
        WorldDispatcher.remove(world.worldName());
        world.reset();
        // Clear all active listeners from prev tests
        SessionEventBus.get().removeAll();
    }

    @Test
    void singleConnection_sendJoinCommand_joinServerSuccess() throws Exception {
        TestClient testClient = null;
        try {
            testClient = new TestClient("localhost", PORT);
            String clientName = "client-1";

            // join server
            String actualJoinResponse = testClient.join(clientName, world.worldName(), 100);
            String expectedJoinResponse = "WELCOME " + clientName;
            // Verify success join
            log.info("[TEST]: Received JOIN response from server: {}", actualJoinResponse);
            assertEquals(expectedJoinResponse, actualJoinResponse, "Unexpected response from server for " + clientName);
        } finally {
            assert testClient != null;
            testClient.close();
        }

        // stop server
        stopServerWhen(sessionManager, server, serverExecutor);

        List<Entity> entities = world.getCurrentEntities();
        log.info("Entities in world: {}", entities);
        assertEquals(0, entities.size());
    }

    @Test
    void singleConnection_sendMoveCommand_moveInTheWorldSuccess() throws Exception {
        try (TestClient testClient = new TestClient("localhost", PORT)) {
            String clientName = "client-1";

            // join server
            String actualJoinResponse = testClient.join(clientName, world.worldName(), 100);
            String expectedJoinResponse = "WELCOME " + clientName;
            // Verify success join
            log.info("[TEST]: Received JOIN response from server: {}", actualJoinResponse);
            assertEquals(expectedJoinResponse, actualJoinResponse, "Unexpected response from server for " + clientName);

            // send move command to server
            log.info("[TEST]: Sending MOVE request to server...");
            String actualMoveResponse = testClient.move(clientName, "UP", 100);
//            String expectedMoveResponse = "ACK UP";
            // ticker will constantly stream the state, ack command is never sent or is lost between state broadcasts
            String expectedMoveResponse = "STATE " + world.serialize();
            log.info("[TEST]: Received MOVE response from server: {}", actualMoveResponse);
            assertEquals(expectedMoveResponse, actualMoveResponse, "Unexpected response from server for " + clientName);
        }
        // stop server
        stopServerWhen(sessionManager, server, serverExecutor);

        List<Entity> entities = world.getCurrentEntities();
        log.info("Entities in world: {}", entities);
        assertEquals(0, entities.size());
    }

    @Test
    void concurrentConnections_sendJoinCommand_joinServerSuccess() throws Exception {
        int clientCount = 3;
        List<TestClient> clients = new ArrayList<>();
        try {
            for (int i = 0; i < clientCount; i++) {
                String clientName = "client-" + i;
                TestClient client = new TestClient("localhost", PORT);
                clients.add(client);
                String actualJoinResponse = client.join(clientName, world.worldName(), 100);
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
        stopServerWhen(sessionManager, server, serverExecutor);

        List<Entity> entities = world.getCurrentEntities();
        log.info("Entities in world: {}", entities);
        assertEquals(0, entities.size());
    }

//    @Test
//    void concurrentConnections_sendMoveCommand_moveInTheWorldSuccess() throws Exception {
//        int clientCount = 3;
//        List<TestClient> clients = new ArrayList<>();
//        try {
//            for (int i = 0; i < clientCount; i++) {
//                String clientName = "client-" + i;
//                TestClient client = new TestClient("localhost", PORT);
//                clients.add(client);
//                String actualJoinResponse = client.join(clientName, world.worldName(), 100);
//                String expectedJoinResponse = "WELCOME " + clientName;
//                assertEquals(expectedJoinResponse, actualJoinResponse);
//
//                // send move command to server
//                log.info("[TEST]: Sending MOVE request to server...");
//                String actualMoveResponse = client.move(clientName, "UP", 100);
//                String expectedMoveResponse = "STATE " + world.serialize();
//                log.info("[TEST]: Received MOVE response from server: {}", actualMoveResponse);
//                assertEquals(expectedMoveResponse, actualMoveResponse, "Unexpected response from server for " + clientName);
//            }
//        } finally {
//            // TestClient is auto-closable, but with simulating multiple clients, close them explicitly
//            for (TestClient client : clients) {
//                client.close();
//            }
//        }
//        // stop server
//        stopServerWhen(sessionManager, server, serverExecutor);
//
//        List<Entity> entities = world.getCurrentEntities();
//        log.info("Entities in world: {}", entities);
//        assertEquals(0, entities.size());
//    }

}