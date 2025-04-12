package com.ntros.server;

import com.ntros.connection.Connection;
import com.ntros.connection.SocketConnection;
import com.ntros.event.listener.SessionEventListener;
import com.ntros.session.ClientSession;
import com.ntros.session.Session;
import com.ntros.event.listener.SessionManager;
import com.ntros.event.bus.EventBus;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Accepts new sockets, creates Connection, spawns sessions
 */
public class TcpServer implements Server {

    private static final Logger LOGGER = Logger.getLogger(TcpServer.class.getName());

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
        LOGGER.log(Level.INFO, "Accepting connections...");

        while (running) {
            try {
                // blocks I/O until connection is received
                Socket socket = serverSocket.accept();

                // once received, create session
                Connection connection = new SocketConnection(socket);
                Session session = new ClientSession(connection, eventBus);

                // process client input in separate thread, unblocks server loop
                Thread.startVirtualThread(session::send);

            } catch (SocketException ex) {
                if (!running) {
                    LOGGER.log(Level.INFO, "[TcpServer]: Server socket closed, exiting accept() loop.");
                    break;
                } else {
                    throw ex; // unexpected error
                }
            }
        }

    }

    @Override
    public void stop() throws IOException {
        LOGGER.log(Level.INFO, "Shutting down server...");
        running = false;
        serverSocket.close();
        sessionManager.shutdownAll();
    }
}
