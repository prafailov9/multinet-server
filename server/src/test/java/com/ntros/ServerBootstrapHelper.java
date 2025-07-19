package com.ntros;

import com.ntros.event.bus.SessionEventBus;
import com.ntros.event.listener.SessionCleaner;
import com.ntros.server.Server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

public class ServerBootstrapHelper {

    public static void startServer(Server server, ExecutorService serverExecutor, int port) {
        serverExecutor.submit(() -> {
            try {
                SessionEventBus.get().register(new SessionCleaner());
                server.start(port);
            } catch (IOException e) {
                System.out.println("Could not start the server: " + e.getMessage());
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static String sendJoinCommand(int port, String clientName) {
        String res = "";
        try (Socket socket = new Socket("localhost", port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
             );
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            // send message to server
            // Each client sends a message terminated by a newline.
            out.println("JOIN " + clientName + "\n");
            out.flush();
            // read server response
            res = in.readLine();
        } catch (IOException ex) {
            System.out.println("Request failed for client: " + clientName);
        }
        return res;
    }

    public static String sendJoinCommand(int port, int clientId) {
        String res = "";
        try (Socket socket = new Socket("localhost", port);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
             );
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            // send message to server
            out.println("JOIN client-" + clientId + "\n");
            out.flush();
            // read server response
            res = in.readLine();
        } catch (IOException ex) {
            System.out.println("Request failed for client: " + clientId);
        }
        return res;
    }

}
