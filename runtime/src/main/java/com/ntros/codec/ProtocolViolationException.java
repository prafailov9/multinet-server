package com.ntros.codec;

import java.io.IOException;

/**
 * Thrown by {@link PacketCodec} when an inbound frame or payload violates the binary protocol:
 * unknown type byte, length field out of range, truncated payload, or a field value that
 * would require an unreasonably large allocation.
 *
 * <p>Extends {@link IOException} so it propagates naturally through the session I/O loop,
 * causing the connection to be closed — the same treatment as a genuine socket error.
 */
public final class ProtocolViolationException extends IOException {

  public ProtocolViolationException(String message) {
    super(message);
  }

  public ProtocolViolationException(String message, Throwable cause) {
    super(message, cause);
  }
}
