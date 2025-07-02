package com.ntros.server;

import com.ntros.connection.Connection;
import com.ntros.connection.SocketConnection;
import com.ntros.event.bus.EventBus;
import com.ntros.event.listener.SessionEventListener;
import com.ntros.event.listener.SessionManager;
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
    private final SessionManager sessionManager;
    private final SessionEventListener connectionEventListener;
    private volatile boolean running = true;

    public TcpServer(SessionManager sessionManager, SessionEventListener connectionEventListener) {
        this.sessionManager = sessionManager;
        this.connectionEventListener = connectionEventListener;
    }

    @Override
    public void start(int port, EventBus eventBus) throws IOException {
        eventBus.register(connectionEventListener);
        serverSocket = new ServerSocket(port);
        log.info("Accepting connections...");

        while (running) {
            try {
                // blocks main thread until connection is received
                Socket socket = serverSocket.accept();

                // once received, create connection + session
                // process client input in separate thread, unblocks server loop
                Thread.startVirtualThread(() -> handleConnection(socket, eventBus));
            } catch (SocketException ex) {
                if (!running) {
                    log.info("Server socket closed, exiting accept() loop.");
                    break;
                } else {
                    throw ex; // unexpected error
                }
            }
        }

    }

    @Override
    public void stop() throws IOException {
        log.info("Shutting down server...");
        running = false;
        serverSocket.close();
        sessionManager.shutdownAll();
    }

    private void handleConnection(Socket socket, EventBus eventBus) {
        try {
            Connection connection = new SocketConnection(socket);
            Session session = new ClientSession(connection, eventBus);

            session.request();
        } catch (Exception ex) {
            log.error("Error occurred during connection handling:", ex);
        }
    }
}
