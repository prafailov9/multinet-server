package com.ntros;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;

import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerTestHelper {

    public static void startServer(Server server, ExecutorService serverExecutor, int port) {
        serverExecutor.submit(() -> {
            try {
                server.start();
            } catch (IOException e) {
                System.out.println("Could not start the server: " + e.getMessage());
            }

            // waiting for port to open
            waitForPort(port, 10000);
        });
    }

    public static void stopServerWhen(SessionManager sessionManager, Server server,
                                      ExecutorService serverExecutor) throws IOException {
        server.stop();

        await()
                .pollDelay(Duration.ofMillis(100)) // Give some room for shutdown event handling
                .pollInterval(Duration.ofMillis(50))
                .atMost(Duration.ofSeconds(5))
                .conditionEvaluationListener(condition -> {
                    int sessions = sessionManager.activeSessionsCount();
                    int entities = WorldConnectorHolder.getDefaultWorld().getCurrentEntities().size();
                    log.info("[Awaitility Poll] Sessions: {}, Entities: {}", sessions, entities);
                })
                .until(() -> {
                    int sessions = sessionManager.activeSessionsCount();
                    int entities = WorldConnectorHolder.getDefaultWorld().getCurrentEntities().size();
                    return sessions == 0 && entities == 0;
                });

        serverExecutor.shutdownNow();
    }

//    public static void stopServerWhen(SessionManager sessionManager, Server server, ExecutorService serverExecutor) throws IOException {
//        server.stop();
//
//        await()
//                .pollDelay(Duration.ofMillis(150)) // wait before first poll
//                .pollInterval(Duration.ofMillis(50))
//                .atMost(Duration.ofSeconds(5))
//                .until(() -> {
//                    int sessions = sessionManager.activeSessions();
//                    int entities = WorldConnectorHolder.getDefaultWorld().getCurrentEntities().size();
//                    log.info("[Awaitility] Sessions: {}, Entities: {}", sessions, entities);
//                    return sessions == 0 && entities == 0;
//                });
//
//
//        serverExecutor.shutdownNow();
//    }

    public static void waitForPort(int port, long timeoutMillis) {
        await()
                .atMost(timeoutMillis, MILLISECONDS)
                .pollInterval(50, MILLISECONDS)
                .ignoreExceptions() // ignore ConnectException until port is open
                .until(() -> {
                    try (Socket ignored = new Socket("localhost", port)) {
                        return true;
                    }
                });
    }

    public static String sendCommand(int port, String clientName, String command) {
        String res = "";
        try (Socket socket = new Socket("localhost", port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
             );
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            // send message to server
            // Each client sends a message terminated by a newline.
            out.println(command);
            out.flush();
            // read server response
            res = in.readLine();
        } catch (IOException ex) {
            System.out.println();
            log.error("Request failed for client: " + clientName + ". Error: " + ex.getMessage(), ex);
        }
        return res;
    }

}
