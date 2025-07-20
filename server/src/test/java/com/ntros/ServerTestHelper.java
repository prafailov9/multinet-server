package com.ntros;

import com.ntros.event.bus.SessionEventBus;
import com.ntros.event.listener.SessionCleaner;
import com.ntros.model.world.Message;
import com.ntros.server.Server;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
public class ServerTestHelper {

    public static void startServer(Server server, ExecutorService serverExecutor, int port) {
        serverExecutor.submit(() -> {
            try {
                server.start(port);
            } catch (IOException e) {
                System.out.println("Could not start the server: " + e.getMessage());
            }

            // waiting for port to open
            waitForPort(port, 10000);
        });
    }

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
