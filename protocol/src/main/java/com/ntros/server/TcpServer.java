package com.ntros.server;

import com.ntros.connection.SocketConnection;
import com.ntros.instance.ins.Instance;
import com.ntros.instance.InstanceRegistry;
import com.ntros.session.ClientSession;
import com.ntros.session.Session;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import lombok.extern.slf4j.Slf4j;

/**
 * Accepts new sockets, creates Connection, spawns sessions
 */
@Slf4j
public class TcpServer implements Server {

  private final int port;

  private ServerSocket serverSocket;
  private volatile boolean running = true;


  public TcpServer(int port) {
    this.port = port;
  }

  @Override
  public void start() throws IOException {
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

    for (Instance instance : InstanceRegistry.getAll()) {
      instance.reset();
    }

    if (serverSocket != null && !serverSocket.isClosed()) {
      try {
        serverSocket.close();
      } catch (IOException ex) {
        log.warn("stop(): error closing serverSocket: {}", ex.getMessage(), ex);
        throw ex;
      }
    } else {
      log.warn("stop(): serverSocket was null or already closed");
    }
  }


  private void startSession(Socket socket) {
    try {
      Session session = new ClientSession(new SocketConnection(socket));

      session.start();
    } catch (Exception ex) {
      log.error("Error occurred during connection handling:", ex);
    }
  }
}
