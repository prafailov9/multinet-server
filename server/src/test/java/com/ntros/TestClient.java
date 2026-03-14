package com.ntros;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Test-side client for sending commands and receiving responses.
 *
 * <p>Handles the mixed server→client protocol:
 * <ul>
 *   <li><b>Text lines</b> (WELCOME, ERROR, ACK) — newline-terminated UTF-8</li>
 *   <li><b>Binary frames</b> (STATE) — {@code [length:4][version:1][type:1][flags:1][reserved:1][payload:N]}</li>
 * </ul>
 *
 * <p>Discrimination is done on the first byte of each incoming message: {@code 0x00} signals a
 * binary frame (the high byte of the big-endian 4-byte {@code length} field); any ASCII letter
 * signals a newline-terminated text line.
 */
public class TestClient implements Closeable {

  /**
   * Wire type byte for STATE packets — must match
   * {@link com.ntros.codec.packet.StatePacket#TYPE}.
   */
  private static final byte STATE_TYPE = 0x03;

  private final Socket socket;
  private final PushbackInputStream pis;
  private final DataInputStream in;
  private final OutputStream out;

  public TestClient(String host, int port) throws IOException {
    this.socket = new Socket(host, port);
    socket.setSoTimeout(5_000);
    this.pis = new PushbackInputStream(socket.getInputStream(), 1);
    this.in  = new DataInputStream(pis);
    this.out = socket.getOutputStream();
  }

  public ServerMessage register(String clientName, String password, int timeoutSeconds) {
    sendText("REG " + clientName + " " + password);
    return readUntilTypes(timeoutSeconds, ServerCmd.REG_SUCCESS, ServerCmd.ERROR);
  }

  public ServerMessage authenticate(String clientName, int timeoutSeconds) {
    sendText("AUTH " + clientName + " ");
    return readUntilTypes(timeoutSeconds, ServerCmd.WELCOME, ServerCmd.ERROR);
  }

  public ServerMessage join(String clientName, String worldName, int timeoutSeconds) {
    sendText("JOIN " + clientName + " " + worldName);
    return readUntilTypes(timeoutSeconds, ServerCmd.WELCOME, ServerCmd.ERROR);
  }

  public ServerMessage move(String clientName, float dx, float dy, float dz, float dw, int timeoutSeconds) {
    sendText(String.format("MOVE %s %s %s %s %s", dx, dy, dz, dw, clientName)
    );
    return readUntilTypes(timeoutSeconds, ServerCmd.STATE, ServerCmd.ERROR);
  }

  /**
   * Sends {@code ORCHESTRATE <subCmd>} and waits for ACK or ERROR.
   * Example: {@code orchestrate("RANDOM 0.7", 2)}.
   */
  public ServerMessage orchestrate(String subCmd, int timeoutSeconds) {
    sendText("ORCHESTRATE " + subCmd);
    return readUntilTypes(timeoutSeconds, ServerCmd.ACK, ServerCmd.ERROR);
  }

  /** Waits up to {@code timeoutSeconds} for the next STATE frame from the server. */
  public ServerMessage waitForState(int timeoutSeconds) {
    return readUntilTypes(timeoutSeconds, ServerCmd.STATE, ServerCmd.ERROR);
  }

  private void sendText(String text) {
    try {
      out.write((text + "\n").getBytes(StandardCharsets.UTF_8));
      out.flush();
    } catch (IOException e) {
      throw new RuntimeException("Send failed", e);
    }
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
          ServerMessage msg = readNextMessage();
          if (msg == null) return false;
          if (targets.contains(msg.type())) {
            holder[0] = msg;
            return true;
          }
          return false; // skip ACK / STATE while waiting for WELCOME, etc.
        });

    return holder[0];
  }

  /**
   * Reads one message from the server stream.
   *
   * <p>Peeks at the first byte to choose a decoder:
   * <ul>
   *   <li>{@code 0x00} — binary frame; delegates to {@link #readBinaryFrame()}</li>
   *   <li>any other byte — text line; pushes the byte back and delegates to
   *       {@link #readTextLine()}</li>
   * </ul>
   *
   * @return the parsed message, or {@code null} on EOF / socket read timeout
   */
  private ServerMessage readNextMessage() throws IOException {
    int firstByte;
    try {
      firstByte = in.read();
    } catch (SocketTimeoutException e) {
      return null;
    }
    if (firstByte == -1) return null;

    if (firstByte == 0x00) {
      return readBinaryFrame();
    } else {
      pis.unread(firstByte);
      return readTextLine();
    }
  }

  /**
   * Reads a binary frame from the stream.
   *
   * <p>The first byte of the 4-byte big-endian {@code length} field (always {@code 0x00}) has
   * already been consumed by the caller; this method reads the remaining 3 bytes to reconstruct
   * {@code length}, then reads the 4-byte header tail and the payload.
   *
   * <p>Frame layout: {@code [length:4][version:1][type:1][flags:1][reserved:1][payload:length-4]}
   * <br>STATE payload: {@code [seq:8][worldNameLen:1][worldName:N][body:M]}
   */
  private ServerMessage readBinaryFrame() throws IOException {
    // Reconstruct length: first byte was 0x00, read the remaining 3 bytes.
    int length = (in.readUnsignedByte() << 16)
               | (in.readUnsignedByte() << 8)
               |  in.readUnsignedByte();

    // Header tail: version(1), type(1), flags(1), reserved(1)
    in.readByte();              // version  — not needed in tests
    byte type = in.readByte();
    in.readByte();              // flags    — not needed in tests
    in.readByte();              // reserved — not needed in tests

    // Payload size = length - 4 (length covers the 4-byte header tail + payload)
    int payloadLen = length - 4;
    byte[] payload = new byte[payloadLen];
    in.readFully(payload);

    if (type == STATE_TYPE) {
      ByteBuffer buf = ByteBuffer.wrap(payload);
      buf.getLong();                            // seq — discard
      int nameLen = buf.get() & 0xFF;
      buf.position(buf.position() + nameLen);   // skip worldName
      byte[] body = new byte[buf.remaining()];
      buf.get(body);
      String json = new String(body, StandardCharsets.UTF_8);
      return new ServerMessage(ServerCmd.STATE, List.of(json), "binary");
    }

    return new ServerMessage(ServerCmd.UNKNOWN, List.of(),
        "binary-0x" + Integer.toHexString(type & 0xFF));
  }

  /** Reads a newline-terminated text line (the first byte has already been pushed back). */
  private ServerMessage readTextLine() throws IOException {
    StringBuilder sb = new StringBuilder();
    int b;
    while ((b = in.read()) != -1) {
      if (b == '\n') break;
      if (b != '\r') sb.append((char) b);
      if (sb.length() > 8_192) throw new IOException("Line too long");
    }
    return ServerMessage.parse(sb.toString());
  }

  @Override
  public void close() {
    try { socket.close(); } catch (IOException ignored) {}
  }

  // ---- test-only helpers ----

  enum ServerCmd { WELCOME, ERROR, STATE, ACK, REG_SUCCESS, UNKNOWN }

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
        case "WELCOME"     -> ServerCmd.WELCOME;
        case "ERROR"       -> ServerCmd.ERROR;
        case "STATE"       -> ServerCmd.STATE;
        case "ACK"         -> ServerCmd.ACK;
        case "REG_SUCCESS" -> ServerCmd.REG_SUCCESS;
        default            -> ServerCmd.UNKNOWN;
      };

      List<String> args = (cmd == ServerCmd.STATE)
          ? List.of(tail)                           // keep JSON blob intact
          : (tail.isBlank() ? List.of() : List.of(tail.split("\\s+")));

      return new ServerMessage(cmd, args, trimmed);
    }
  }
}
