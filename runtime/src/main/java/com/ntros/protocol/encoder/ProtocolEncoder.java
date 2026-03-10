package com.ntros.protocol.encoder;

public interface ProtocolEncoder {

  /** Legacy text-protocol path — encodes the full {@link StateFrame} as a "STATE <json>\n" line. */
  String encodeState(StateFrame frame);

  /**
   * Binary-protocol path — serializes only the domain snapshot object into raw bytes.
   * The frame metadata (version, world name, seq) are carried by the binary frame header and
   * {@link com.ntros.codec.packet.StatePacket} fields; they must NOT be included here.
   *
   * @param data domain snapshot (e.g. {@code GridSnapshot})
   * @return UTF-8 JSON bytes of {@code data}, never null
   */
  byte[] encodeBody(Object data);
}
