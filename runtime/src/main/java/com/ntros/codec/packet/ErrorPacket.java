package com.ntros.codec.packet;

import java.util.Objects;

/**
 * ERROR (0x04) — server→client error notification.
 *
 * <p>Payload layout (inside the binary frame):
 * <pre>
 *   [ errorCode:2 ][ msgLen:2 ][ message:msgLen ]
 * </pre>
 *
 * <ul>
 *   <li>{@code errorCode} — 2-byte signed short identifying the error class. Use the
 *                           {@code static final} constants defined below.</li>
 *   <li>{@code message}   — human-readable UTF-8 detail string (max 65 535 bytes via msgLen
 *                           field; codec enforces a tighter ceiling of 1 MB).</li>
 * </ul>
 *
 * <h3>Error codes</h3>
 * Codes are grouped by category:
 * <ul>
 *   <li>0x00xx — protocol-level errors</li>
 *   <li>0x01xx — world/session errors</li>
 * </ul>
 */
public record ErrorPacket(short errorCode, String message) implements Packet {

  public static final byte TYPE = 0x04;

  // --- 0x00xx: protocol-level -------------------------------------------

  /**
   * Received a command token the server does not recognise.
   */
  public static final short UNKNOWN_COMMAND = 0x0001;

  // --- 0x01xx: world / session ------------------------------------------

  /**
   * The named world does not exist or is not registered.
   */
  public static final short WORLD_NOT_FOUND = 0x0101;

  /**
   * Join was accepted but the actor returned a failure result.
   */
  public static final short JOIN_FAILED = 0x0102;

  /**
   * Client attempted a world action before completing AUTH.
   */
  public static final short NOT_AUTHENTICATED = 0x0103;

  /**
   * The world is marked PRIVATE and the caller is not the owner.
   */
  public static final short WORLD_PRIVATE = 0x0104;

  /**
   * Single-player world already has an active session.
   */
  public static final short WORLD_BUSY = 0x0105;

  /**
   * The actor did not respond to a join request within the timeout window.
   */
  public static final short JOIN_TIMEOUT = 0x0106;

  // --- 0x02xx: server-internal ------------------------------------------

  /**
   * Catch-all for unexpected server-side failures.
   */
  public static final short INTERNAL_ERROR = 0x0201;

  // ----------------------------------------------------------------------

  public ErrorPacket {
    Objects.requireNonNull(message, "message must not be null");
  }

  @Override
  public byte type() {
    return TYPE;
  }

  /**
   * Convenience factory — avoids casting call-sites when the code is a named constant.
   */
  public static ErrorPacket of(short code, String message) {
    return new ErrorPacket(code, message);
  }
}
