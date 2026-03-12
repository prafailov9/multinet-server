package com.ntros.lifecycle.session;

import com.ntros.lifecycle.Lifecycle;
import com.ntros.lifecycle.Shutdownable;

/**
 * Represents an active connection between a client and the server.
 */
public interface Session extends Lifecycle, Shutdownable {

  SessionContext getSessionContext();

  /**
   * Sends a text command response to the client (legacy text-protocol path).
   * The implementation appends {@code \n} before writing to the wire.
   */
  void response(String serverResponse);

  /**
   * Sends a pre-encoded binary frame to the client.
   * The frame is written verbatim — no bytes are added by the implementation.
   * Use this for all frames produced by {@link com.ntros.codec.PacketCodec}.
   */
  void response(byte[] frame);

  /** Signals the session to stop after completing the current message. */
  void shutdown();
}
