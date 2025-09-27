package com.ntros;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.ntros.connection.Connection;
import com.ntros.connection.SocketConnection;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Predicate;

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

  private final Socket socket;
  private final Connection conn;

  public TestClient(String host, int port) throws IOException {
    this.socket = new Socket();
    // Connect with a short timeout so tests fail fast if server isn't up yet
    socket.connect(new InetSocketAddress(host, port), 1_000);
    this.conn = new SocketConnection(socket);
  }
  // ---------------- High-level protocol helpers ----------------

  /** Sends JOIN and waits specifically for a WELCOME line. */
  public String join(String clientName, String worldName, long timeoutMs) throws IOException {
    sendLine("JOIN " + clientName + " " + worldName);
    return readUntilPrefix("WELCOME ", timeoutMs);
  }

  /** Sends MOVE and waits for the next STATE broadcast, ignoring ACKs. */
  public String move(String clientName, String direction, long timeoutMs) throws IOException {
    sendLine("MOVE " + direction + " " + clientName);
    return readUntilPrefix("STATE ", timeoutMs);
  }

  /** Generic send (adds newline via SocketConnection). */
  public void sendLine(String line) {
    conn.send(line);
  }

  // ---------------- Read helpers using SocketConnection ----------------

  /**
   * Read lines until one starts with the given prefix, or the deadline elapses.
   * Lines like "_TIMEOUT_" (from SocketConnection) are treated as "no data yet" and ignored.
   */
  public String readUntilPrefix(String prefix, long timeoutMs) throws IOException {
    Objects.requireNonNull(prefix, "prefix");
    long deadline = System.nanoTime() + timeoutMs * 1_000_000L;

    return readUntil(s -> s.startsWith(prefix), deadline);
  }

  private String readUntil(Predicate<String> match, long deadlineNanos) throws IOException {
    for (;;) {
      String line = conn.receive();
      if (line == null || line.isEmpty() || "_TIMEOUT_".equals(line)) {
        // No data yet; check deadline then keep polling
        if (System.nanoTime() >= deadlineNanos) {
          throw new IOException("read timeout waiting for expected message");
        }
        // Small yield to avoid tight spinning
        try { Thread.sleep(2); } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new IOException("interrupted", ie);
        }
        continue;
      }

      // Normalize any stray CR (receive() already strips it, but be safe)
      String normalized = new String(line.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

      if (match.test(normalized)) {
        return normalized;
      }
      // Otherwise ignore (e.g., ACK) and continue until deadline
      if (System.nanoTime() >= deadlineNanos) {
        throw new IOException("read timeout before matching expected message; last='" + normalized + "'");
      }
    }
  }

  @Override
  public void close() throws IOException {
    try { conn.close(); } finally {
      // SocketConnection.close() already closes the socket, but be defensive:
      try { socket.close(); } catch (Exception ignore) {}
    }
  }
}
