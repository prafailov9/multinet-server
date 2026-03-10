package com.ntros.codec;

import com.ntros.codec.packet.ErrorPacket;
import com.ntros.codec.packet.MovePacket;
import com.ntros.codec.packet.Packet;
import com.ntros.codec.packet.StatePacket;
import com.ntros.codec.packet.WelcomePacket;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Stateless binary protocol codec — encodes {@link Packet} objects into wire frames and decodes
 * raw payloads back into typed packets.
 *
 * <h3>Frame layout</h3>
 * <pre>
 *   Offset  Size  Field
 *   0       4     length   — total bytes after this field: 4 (header tail) + payload.length
 *   4       1     version  — always {@link #VERSION}
 *   5       1     type     — packet type byte (see each Packet class for its TYPE constant)
 *   6       1     flags    — reserved for future use (0x00 today)
 *   7       1     reserved — always 0x00
 *   8       N     payload  — type-specific bytes (see individual decode* methods)
 * </pre>
 *
 * <h3>Usage</h3>
 * <p>{@code PacketCodec} is stateless and thread-safe. Share a single instance per application.
 *
 * <h3>Reading from the wire</h3>
 * The caller is responsible for framing before invoking {@link #decode}:
 * <ol>
 *   <li>Read 4 bytes → {@code length}</li>
 *   <li>Reject if {@code length < 4} or {@code length > MAX_FRAME_LENGTH}</li>
 *   <li>Read exactly {@code length} bytes → header tail + payload</li>
 *   <li>Extract {@code version}, {@code type}, {@code flags}, {@code reserved}, {@code payload}</li>
 *   <li>Call {@code decode(type, payload)}</li>
 * </ol>
 */
public final class PacketCodec {

  public static final byte VERSION = 1;

  /**
   * Hard ceiling on any single payload. Guards against crafted frames that claim a large
   * {@code msgLen} or similar length field to force a large heap allocation before validation.
   */
  static final int MAX_PAYLOAD_SIZE = 1 << 20; // 1 MB

  // -------------------------------------------------------------------------
  // Encode
  // -------------------------------------------------------------------------

  /**
   * Encodes a {@link Packet} into a complete binary frame ready to write to the wire.
   *
   * @param packet the packet to encode
   * @return full frame bytes (8-byte header + payload)
   * @throws ProtocolViolationException if no encoder exists for the packet's type
   */
  public byte[] encode(Packet packet) throws IOException {
    return switch (packet) {
      case StatePacket p -> encodeState(p);
      case ErrorPacket p -> encodeError(p);
      case WelcomePacket p -> encodeWelcome(p);
      case MovePacket p -> encodeMovePacket(p);
      default -> throw new ProtocolViolationException(
          "No encoder for packet type: 0x%02X".formatted(packet.type() & 0xFF));
    };
  }

  // -- STATE ----------------------------------------------------------------

  /**
   * STATE payload:
   * <pre>[ seq:8 ][ worldNameLen:1 ][ worldName:N ][ body:M ]</pre>
   */
  private static byte[] encodeState(StatePacket p) {
    byte[] nameBytes = p.worldName().getBytes(StandardCharsets.UTF_8);
    ByteBuffer buf = ByteBuffer.allocate(8 + 1 + nameBytes.length + p.body().length);
    buf.putLong(p.seq());
    buf.put((byte) nameBytes.length);
    buf.put(nameBytes);
    buf.put(p.body());
    return buildFrame(StatePacket.TYPE, (byte) 0x00, buf.array());
  }

  // -- ERROR ----------------------------------------------------------------

  /**
   * ERROR payload:
   * <pre>[ errorCode:2 ][ msgLen:2 ][ message:msgLen ]</pre>
   */
  private static byte[] encodeError(ErrorPacket p) {
    byte[] msgBytes = p.message().getBytes(StandardCharsets.UTF_8);
    ByteBuffer buf = ByteBuffer.allocate(2 + 2 + msgBytes.length);
    buf.putShort(p.errorCode());
    buf.putShort((short) msgBytes.length);
    buf.put(msgBytes);
    return buildFrame(ErrorPacket.TYPE, (byte) 0x00, buf.array());
  }

  // -- WELCOME --------------------------------------------------------------

  /**
   * WELCOME payload:
   * <pre>[ playerId:4 ]</pre>
   */
  private static byte[] encodeWelcome(WelcomePacket p) {
    ByteBuffer buf = ByteBuffer.allocate(4);
    buf.putInt(p.playerId());
    return buildFrame(WelcomePacket.TYPE, (byte) 0x00, buf.array());
  }

  // -- MOVE -----------------------------------------------------------------

  /**
   * MOVE payload:
   * <pre>[ direction:1 ]</pre>
   */
  private static byte[] encodeMovePacket(MovePacket p) {
    return buildFrame(MovePacket.TYPE, (byte) 0x00, new byte[]{p.direction()});
  }

  // -------------------------------------------------------------------------
  // Decode
  // -------------------------------------------------------------------------

  /**
   * Decodes a raw payload into a typed {@link Packet}.
   *
   * <p>The caller must have already stripped the 8-byte frame header (length + version + type +
   * flags + reserved) and extracted {@code type} from it. {@code payload} contains only the
   * application-level bytes that follow the header.
   *
   * @param type    the packet type byte from the frame header
   * @param payload the application payload bytes (header already stripped)
   * @return decoded packet
   * @throws ProtocolViolationException if the type is unknown or the payload is malformed
   */
  public Packet decode(byte type, byte[] payload) throws IOException {
    return switch (type) {
      case StatePacket.TYPE -> decodeState(payload);
      case ErrorPacket.TYPE -> decodeError(payload);
      case WelcomePacket.TYPE -> decodeWelcome(payload);
      case MovePacket.TYPE -> decodeMove(payload);
      default -> throw new ProtocolViolationException(
          "Unknown packet type: 0x%02X".formatted(type & 0xFF));
    };
  }

  // -- STATE ----------------------------------------------------------------

  private static StatePacket decodeState(byte[] payload) throws ProtocolViolationException {
    // minimum: seq(8) + nameLen(1) = 9 bytes
    if (payload.length < 9) {
      throw new ProtocolViolationException(
          "STATE payload too short: " + payload.length + " bytes (need ≥ 9)");
    }
    ByteBuffer buf = ByteBuffer.wrap(payload);
    long seq = buf.getLong();
    int nameLen = buf.get() & 0xFF;

    if (nameLen > buf.remaining()) {
      throw new ProtocolViolationException(
          "STATE worldNameLen (" + nameLen + ") exceeds remaining payload ("
              + buf.remaining() + ")");
    }
    byte[] nameBytes = new byte[nameLen];
    buf.get(nameBytes);

    byte[] body = new byte[buf.remaining()];
    buf.get(body);

    return new StatePacket(seq, new String(nameBytes, StandardCharsets.UTF_8), body);
  }

  // -- ERROR ----------------------------------------------------------------

  private static ErrorPacket decodeError(byte[] payload) throws ProtocolViolationException {
    // minimum: errorCode(2) + msgLen(2) = 4 bytes
    if (payload.length < 4) {
      throw new ProtocolViolationException(
          "ERROR payload too short: " + payload.length + " bytes (need ≥ 4)");
    }
    ByteBuffer buf = ByteBuffer.wrap(payload);
    short errorCode = buf.getShort();
    int msgLen = buf.getShort() & 0xFFFF; // treat as unsigned

    // Guard before any allocation: reject suspiciously large claimed lengths.
    if (msgLen > MAX_PAYLOAD_SIZE) {
      throw new ProtocolViolationException(
          "ERROR msgLen (" + msgLen + ") exceeds MAX_PAYLOAD_SIZE (" + MAX_PAYLOAD_SIZE + ")");
    }
    if (msgLen > buf.remaining()) {
      throw new ProtocolViolationException(
          "ERROR msgLen (" + msgLen + ") exceeds remaining payload (" + buf.remaining() + ")");
    }
    byte[] msgBytes = new byte[msgLen];
    buf.get(msgBytes);

    return new ErrorPacket(errorCode, new String(msgBytes, StandardCharsets.UTF_8));
  }

  // -- WELCOME --------------------------------------------------------------

  private static WelcomePacket decodeWelcome(byte[] payload) throws ProtocolViolationException {
    if (payload.length < 4) {
      throw new ProtocolViolationException(
          "WELCOME payload too short: " + payload.length + " bytes (need ≥ 4)");
    }
    return new WelcomePacket(ByteBuffer.wrap(payload).getInt());
  }

  // -- MOVE -----------------------------------------------------------------

  private static MovePacket decodeMove(byte[] payload) throws ProtocolViolationException {
    if (payload.length < 1) {
      throw new ProtocolViolationException("MOVE payload is empty");
    }
    return new MovePacket(payload[0]);
  }

  // -------------------------------------------------------------------------
  // Frame builder
  // -------------------------------------------------------------------------

  /**
   * Builds a complete binary frame.
   *
   * <pre>
   *   [ length:4 ][ version:1 ][ type:1 ][ flags:1 ][ reserved:1 ][ payload:N ]
   *   length = 4 (header tail) + payload.length
   * </pre>
   */
  private static byte[] buildFrame(byte type, byte flags, byte[] payload) {
    int length = 4 + payload.length;       // header tail (4) + payload
    ByteBuffer frame = ByteBuffer.allocate(4 + length);
    frame.putInt(length);
    frame.put(VERSION);
    frame.put(type);
    frame.put(flags);
    frame.put((byte) 0x00);                // reserved
    frame.put(payload);
    return frame.array();
  }
}
