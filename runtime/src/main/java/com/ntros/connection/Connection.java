package com.ntros.connection;

import java.io.IOException;

/**
 * Transport-level abstraction. Handles raw per-client I/O.
 *
 * <p>Send side is split by protocol:
 * <ul>
 *   <li>{@link #send(String)} — legacy text path; appends {@code \n} and enqueues.</li>
 *   <li>{@link #send(byte[])} — binary path; enqueues the frame bytes verbatim (no suffix added).
 *       Use this for all {@link com.ntros.codec.PacketCodec}-encoded frames.</li>
 * </ul>
 *
 * <p>Both send methods are non-blocking and thread-safe.
 */
public interface Connection {

  /** Enqueues {@code message + '\n'} for async delivery. Non-blocking. */
  void send(String message);

  /**
   * Enqueues a pre-encoded binary frame for async delivery. No bytes are added or removed.
   * Non-blocking.
   */
  void send(byte[] frame);

  /** Reads one newline-terminated line. Blocks until data arrives or a timeout fires. */
  String receive();

  /** Reads exactly {@code length} bytes. Blocks. */
  byte[] receiveBytesExactly(int length) throws IOException;

  void close();

  boolean isOpen();
}
