package com.ntros;


import com.ntros.event.bus.SessionEventBus;
import com.ntros.event.listener.ClientSessionEventListener;
import com.ntros.event.listener.ClientSessionManager;
import com.ntros.event.listener.SessionEventListener;
import com.ntros.event.listener.SessionManager;
import com.ntros.server.TcpServer;
import com.ntros.server.scheduler.WorldTickScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.ntros.ServerBootstrapHelper.sendJoinCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerBootstrapTest {

    private static final int PORT = 5555;
    private TcpServer server;

    private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();


    @BeforeEach
    void setUp() {
        SessionManager sessionManager = new ClientSessionManager();
        WorldTickScheduler worldTickScheduler = new WorldTickScheduler();

        SessionEventListener clientSessionEventListener = new ClientSessionEventListener(sessionManager, worldTickScheduler);
        SessionEventBus.get().register(clientSessionEventListener);
        server = new TcpServer(sessionManager);

        // Start the server in a background thread.
        ServerBootstrapHelper.startServer(server, serverExecutor, PORT);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Stop the server and shut down the executor.
        server.stop();
        serverExecutor.shutdownNow();
    }

    @Test
    void testSingleClientConnection_sendJoinCommand_joinServerSuccess() throws Exception {
        ExecutorService clientExecutor = Executors.newSingleThreadExecutor();
        String clientName = "client-1";
        // Simulate several clients connecting concurrently.
        Future<String> result = clientExecutor.submit(() -> sendJoinCommand(PORT, clientName));

        // Verify that each client received the expected response.
        String expectedServerResponse = "WELCOME " + clientName;
        String actual = result.get();
        assertEquals(expectedServerResponse, actual, "Unexpected response from server for " + clientName);

        // cleanup
        clientExecutor.shutdownNow();
    }

    @Test
    void testConcurrentClientConnections_sendJoinCommand_joinServerSuccess() throws Exception {
        int clientCount = 3;
        ExecutorService clientExecutor = Executors.newFixedThreadPool(clientCount);
        List<Future<String>> clientResults = new ArrayList<>();

        // Simulate several clients connecting concurrently.
        for (int i = 0; i < clientCount; i++) {
            final String clientName = "client-" + i;

            Future<String> result = clientExecutor.submit(() -> sendJoinCommand(PORT, clientName));
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