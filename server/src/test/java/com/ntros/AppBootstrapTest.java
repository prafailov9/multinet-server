package com.ntros;


import com.ntros.event.listener.SessionCleaner;
import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.WorldDispatcher;
import com.ntros.model.world.context.WorldContext;
import com.ntros.server.TcpServer;
import com.ntros.event.bus.SessionEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppBootstrapTest {

    private static final int PORT = 5555;
    private TcpServer server;
    private ExecutorService serverExecutor;

    @BeforeEach
    void setUp() throws Exception {
        // Instantiate your server abstraction.
        server = new TcpServer(new SessionManager());
        serverExecutor = Executors.newSingleThreadExecutor();

        // Start the server in a background thread.
        startServer();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Stop the server and shut down the executor.
        server.stop();
        serverExecutor.shutdownNow();
    }

    @Test
    public void testMultClients() throws ExecutionException, InterruptedException {
        // create tasks
        FutureTask<String> task1 = new FutureTask<>(task(1));
        FutureTask<String> task2 = new FutureTask<>(task(2));
        FutureTask<String> task3 = new FutureTask<>(task(3));

        // create threads
        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        Thread t3 = new Thread(task3);

        // start threads
        t1.start();
        t2.start();
        t3.start();

        // block get results
        String taskResult1 = task1.get();
        String taskResult2 = task2.get();
        String taskResult3 = task3.get();

        System.out.println("Res1=" + taskResult1);
        System.out.println("Res2=" + taskResult2);
        System.out.println("Res3=" + taskResult3);

        WorldContext worldContext = WorldDispatcher.getWorld("world-1");
        System.out.println(worldContext.engine().serialize(worldContext.state()));
    }

    private Callable<String> task(int id) {
        return () -> {
            String res = "";
            try (Socket socket = new Socket("localhost", PORT);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                 );
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

                // send message to server
                out.println("JOIN client-" + id + "\n");
                out.flush();
                // read server response
                res = in.readLine();
            } catch (IOException ex) {
                System.out.println("Task failed for client: " + id);
            }
            return res;
        };
    }

    @Test
    void testMultipleClients() throws Exception {
        int clientCount = 3;
        ExecutorService clientExecutor = Executors.newFixedThreadPool(clientCount);
        List<Future<String>> clientResults = new ArrayList<>();

        // Simulate several clients connecting concurrently.
        for (int i = 0; i < clientCount; i++) {
            final String clientName = "client-" + i;

            Future<String> result = clientExecutor.submit(() -> {
                try (Socket socket = new Socket("localhost", PORT);
                     BufferedReader in = new BufferedReader(
                             new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                     );
                     PrintWriter out = new PrintWriter(
                             new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
                ) {
                    // Each client sends a message terminated by a newline.
                    String message = "JOIN " + clientName;
                    out.println(message);
                    out.flush();
                    // Read the response from the server.
                    return in.readLine();
                }
            });
            clientResults.add(result);
        }

        // Verify that each client received the expected response.
        for (int i = 0; i < clientCount; i++) {
            String expectedServerResponse = "WELCOME client-" + i;
            String actual = clientResults.get(i).get();
            assertEquals(expectedServerResponse, actual, "Unexpected response from server for client " + i);
        }
        clientExecutor.shutdownNow();
    }

    private void startServer() throws InterruptedException {
        serverExecutor.submit(() -> {
            try {
                SessionEventBus sessionEventBus = new SessionEventBus();
                sessionEventBus.register(new SessionCleaner());
                sessionEventBus.register(new SessionManager());
                server.start(PORT, new SessionEventBus());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @Test
    public void startServerTest() {
        AppBootstrap.startServer();
    }

}