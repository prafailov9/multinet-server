package com.ntros;


import com.ntros.event.bus.SessionEventBus;
import com.ntros.event.listener.*;
import com.ntros.server.TcpServer;
import com.ntros.server.scheduler.WorldTickScheduler;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.ntros.ServerTestHelper.sendCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ServerBootstrapTest {

    private static final int PORT = 5555;
    private static final int TICK_RATE = 120;
    private static final String WHITESPACE = " ";
    private static final String NEW_LINE = "\n";
    private static final String JOIN_COMMAND_TEMPLATE = "JOIN" + WHITESPACE + "%s" + NEW_LINE;
    private TcpServer server;
    private final SessionManager sessionManager = new ClientSessionManager();
    private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();


    @BeforeEach
    void setUp() {
        WorldTickScheduler worldTickScheduler = new WorldTickScheduler(TICK_RATE);

        SessionEventListener clientSessionEventListener = new ClientSessionEventListener(sessionManager, worldTickScheduler);
        SessionEventBus.get().register(clientSessionEventListener);
        SessionEventBus.get().register(new SessionCleaner());

        server = new TcpServer(sessionManager);

        // Start the server in a background thread.
        ServerTestHelper.startServer(server, serverExecutor, PORT);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Stop the server and shut down the executor.
        server.stop();

        // tearDown() might not wait enough for all sessions to close and server threads to complete.
        Awaitility.await()
                .atMost(Duration.ofSeconds(1))
                .until(() -> sessionManager.activeSessions() == 0);

        serverExecutor.shutdownNow();
    }

    @Test
    void singleConnection_sendJoinCommand_joinServerSuccess() throws Exception {

        ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
        String clientName = "client-1";
        String joinCommand = String.format(JOIN_COMMAND_TEMPLATE, clientName);

        // Simulate several clients connecting concurrently.
        Future<String> result = clientExecutor.submit(() -> sendCommand(PORT, clientName, joinCommand));

        // Verify that each client received the expected response.
        String expectedServerResponse = "WELCOME " + clientName;
        String actual = result.get();
        assertEquals(expectedServerResponse, actual, "Unexpected response from server for " + clientName);

        // cleanup
        clientExecutor.shutdownNow();
    }

    @Test
    void singleConnection_sendMoveCommand_moveInTheWorldSuccess() throws Exception {
        try (TestClient testClient = new TestClient("localhost", PORT)) {
            String clientName = "client-1";

            // join server
            String actualJoinResponse = testClient.join(clientName, 1);
            String expectedJoinResponse = "WELCOME " + clientName;
            // Verify success join
            log.info("[TEST]: Received JOIN response from server: {}", actualJoinResponse);
            assertEquals(expectedJoinResponse, actualJoinResponse, "Unexpected response from server for " + clientName);

            // send move command to server
            log.info("[TEST]: Sending MOVE request to server...");
            String actualMoveResponse = testClient.move(clientName, "UP", 1);
            String expectedMoveResponse = "ACK UP";

            log.info("[TEST]: Received MOVE response from server: {}", actualMoveResponse);
            assertEquals(expectedMoveResponse, actualMoveResponse, "Unexpected response from server for " + clientName);
        }
    }

    @Test
    void concurrentConnections_sendJoinCommand_joinServerSuccess() throws Exception {
        int clientCount = 3;
        ExecutorService clientExecutor = Executors.newFixedThreadPool(clientCount);
        List<Future<String>> clientResults = new ArrayList<>();

        // Simulate several clients connecting concurrently.
        for (int i = 0; i < clientCount; i++) {
            String clientName = "client-" + i;
            String joinCommand = String.format(JOIN_COMMAND_TEMPLATE, clientName);

            Future<String> result = clientExecutor.submit(() -> sendCommand(PORT, clientName, joinCommand));
            clientResults.add(result);
        }

        // Verify that each client received the expected response.
        for (int i = 0; i < clientCount; i++) {
            String expectedServerResponse = "WELCOME client-" + i;
            String actual = clientResults.get(i).get();
            assertEquals(expectedServerResponse, actual, "Unexpected response from server for client " + i);
        }

        // cleanup
        clientExecutor.shutdownNow();
    }

}