package com.ntros.server;

import com.ntros.connection.Connection;
import com.ntros.connection.SocketConnection;
import com.ntros.session.ClientSession;
import com.ntros.session.Session;
import com.ntros.session.event.SessionManager;
import com.ntros.session.event.bus.EventBus;

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
    private volatile boolean running = true;

    public TcpServer(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void start(int port, EventBus eventBus) throws IOException {
        eventBus.register(sessionManager);
        serverSocket = new ServerSocket(port);
        LOGGER.log(Level.INFO, "Accepting connections...");

        while (running) {
            try {
                // accepts client connections and creates sessions
                Socket socket = serverSocket.accept();
                Connection connection = new SocketConnection(socket);
                Session session = new ClientSession(connection, eventBus);
                // runs the client session in a managed thread pool
                Thread.startVirtualThread(session::run);
//                clientExecutor.submit(session::run);

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
