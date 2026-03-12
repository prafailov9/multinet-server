package com.ntros.broadcast;

import com.ntros.lifecycle.sessionmanager.SessionManager;

/**
 * Pushes a pre-encoded binary frame to a set of sessions.
 *
 * <p>The frame must be a complete, wire-ready byte array produced by
 * {@link com.ntros.codec.PacketCodec}. Implementations must not modify the bytes.
 */
public interface Broadcaster {

  void publish(byte[] frame, SessionManager sessions);
}
