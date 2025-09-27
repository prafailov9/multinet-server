package com.ntros;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.ntros.connection.SocketConnection;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
    this.connection = new SocketConnection(socket, true);
  }

  public ServerMessage join(String clientName, String worldName, int timeoutSeconds) {
    connection.send("JOIN " + clientName + " " + worldName);
    return readUntilTypes(timeoutSeconds, ServerCmd.WELCOME, ServerCmd.ERROR);
  }

  public ServerMessage move(String clientName, String direction, int timeoutSeconds) {
    connection.send("MOVE " + direction + " " + clientName);
    return readUntilTypes(timeoutSeconds, ServerCmd.STATE, ServerCmd.ERROR);
  }

  private ServerMessage readUntilTypes(int timeoutSeconds, ServerCmd... wanted) {
    Set<ServerCmd> targets = EnumSet.noneOf(ServerCmd.class);
    targets.addAll(Arrays.asList(wanted));
    final ServerMessage[] holder = new ServerMessage[1];

    await()
        .atMost(timeoutSeconds, SECONDS)
        .pollInterval(25, MILLISECONDS)
        .ignoreExceptions()
        .until(() -> {
          String line = connection.receive();
          if (line == null || line.isBlank() || "_TIMEOUT_".equals(line)) return false;
          ServerMessage msg = ServerMessage.parse(line);
          if (targets.contains(msg.type())) { holder[0] = msg; return true; }
          return false; // ignore other lines (ACK/STATE while waiting for WELCOME, etc.)
        });

    return holder[0];
  }

  @Override
  public void close() {
    connection.close();
  }

  ///  --- Test Helper Structures

  // test-only model
  enum ServerCmd {WELCOME, ERROR, STATE, ACK, UNKNOWN}

  record ServerMessage(ServerCmd type, List<String> args, String raw) {

    static ServerMessage parse(String line) {
      if (line == null || line.isBlank()) {
        return new ServerMessage(ServerCmd.UNKNOWN, List.of(), "");
      }
      String trimmed = line.strip();
      int sp = trimmed.indexOf(' ');
      String head = sp < 0 ? trimmed : trimmed.substring(0, sp);
      String tail = sp < 0 ? "" : trimmed.substring(sp + 1);

      ServerCmd cmd = switch (head) {
        case "WELCOME" -> ServerCmd.WELCOME;
        case "ERROR" -> ServerCmd.ERROR;
        case "STATE" -> ServerCmd.STATE;
        case "ACK" -> ServerCmd.ACK;
        default -> ServerCmd.UNKNOWN;
      };

      List<String> args = (cmd == ServerCmd.STATE)
          ? List.of(tail)                          // keep JSON blob intact
          : (tail.isBlank() ? List.of() : List.of(tail.split("\\s+")));

      return new ServerMessage(cmd, args, trimmed);
    }
  }

}
