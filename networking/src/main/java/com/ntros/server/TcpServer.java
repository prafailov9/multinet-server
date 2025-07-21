package com.ntros.server;

import com.ntros.connection.Connection;
import com.ntros.connection.SocketConnection;
import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.runtime.Instance;
import com.ntros.runtime.InstanceRegistry;
import com.ntros.server.scheduler.WorldTickScheduler;
import com.ntros.session.ClientSession;
import com.ntros.session.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Accepts new sockets, creates Connection, spawns sessions
 */
@Slf4j
public class TcpServer implements Server {
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private final WorldTickScheduler worldTickScheduler;

    public TcpServer(WorldTickScheduler worldTickScheduler) {
        this.worldTickScheduler = worldTickScheduler;
    }

    @Override
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        log.info("Accepting connections...");

        while (running) {
            try {
                // blocks main thread until connection is received
                Socket socket = serverSocket.accept();

                // once received, create connection + session
                // process client input in separate thread, unblocks server loop
                Thread.startVirtualThread(() -> startSession(socket));
            } catch (SocketException ex) {
                if (!running) {
                    log.info("Server socket closed, exiting accept() loop.");
                    break;
                } else {
                    log.info("Unexpected exit accept loop: {}", ex.getMessage());
                    throw ex; // unexpected error
                }
            }
        }

    }

    @Override
    public void stop() throws IOException {
        log.info("Shutting down server...");
        running = false;

        worldTickScheduler.shutdownInstances();
        worldTickScheduler.stop();
        serverSocket.close();
    }

    private void startSession(Socket socket) {
        try {
            Connection connection = new SocketConnection(socket);
            Session session = new ClientSession(connection);

            session.run();
        } catch (Exception ex) {
            log.error("Error occurred during connection handling:", ex);
        }
    }
}
