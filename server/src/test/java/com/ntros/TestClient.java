package com.ntros;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.ntros.connection.SocketConnection;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

/**
 * Test-side client for sending commands and receiving responses using the real SocketConnection
 * class.
 */
public class TestClient implements Closeable {

  private static final String WHITESPACE = " ";
  private static final String NEW_LINE = "\n";
  private static final String JOIN_COMMAND_TEMPLATE =
      "JOIN" + WHITESPACE + "%s" + WHITESPACE + "%s" + NEW_LINE;
  private static final String MOVE_COMMAND_TEMPLATE =
      "MOVE" + WHITESPACE + "%s" + WHITESPACE + "%s" + NEW_LINE;
  private final SocketConnection connection;

  public TestClient(String host, int port) throws IOException {
    Socket socket = new Socket(host, port);
    this.connection = new SocketConnection(socket);
  }

  public String join(String clientName, String worldName, int timeoutSeconds) {
    connection.send(String.format(JOIN_COMMAND_TEMPLATE, clientName, worldName));
    return timeoutSeconds <= 0 ? connection.receive() : awaitResponse(timeoutSeconds);
  }

  public String move(String clientName, String direction, int timeoutSeconds) {
    connection.send(String.format(MOVE_COMMAND_TEMPLATE, direction, clientName));
    return timeoutSeconds <= 0 ? connection.receive() : awaitResponse(timeoutSeconds);
  }

  public String awaitResponse(int timeoutSeconds) {
    final String[] responseHolder = new String[1];

    await()
        .atMost(timeoutSeconds, SECONDS)
        .pollInterval(50, MILLISECONDS)
        .ignoreExceptions()
        .until(() -> {
          String response = connection.receive();
          // wait for a valid server response
          if (response != null && !response.isBlank()) {
            responseHolder[0] = response;
            return true;
          }
          return false;
        });

    return responseHolder[0];
  }

  public String readBlocking() {
    return connection.receive();
  }

  public boolean isConnected() {
    return connection.isOpen();
  }

  @Override
  public void close() {
    connection.close();
  }
}
