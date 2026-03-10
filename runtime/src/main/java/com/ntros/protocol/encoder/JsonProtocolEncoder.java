package com.ntros.protocol.encoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;

public final class JsonProtocolEncoder implements ProtocolEncoder {

  private static final byte[] ENCODE_ERROR_BYTES =
      "{\"_encode_error_\":true}".getBytes(StandardCharsets.UTF_8);

  private final ObjectMapper mapper = new ObjectMapper();

  /** Legacy text-protocol path. Produces {@code "STATE <json>"} for the line-based wire format. */
  @Override
  public String encodeState(StateFrame frame) {
    try {
      return "STATE " + mapper.writeValueAsString(frame);
    } catch (JsonProcessingException e) {
      // fallback so we never break the stream
      return "STATE {\"proto\":" + frame.proto() + ",\"inst\":\"" + frame.inst()
          + "\",\"seq\":" + frame.seq() + ",\"data\":\"_encode_error_\"}";
    }
  }

  /**
   * Binary-protocol path. Serializes only the domain snapshot to JSON bytes.
   * Frame metadata (version, world name, seq) are handled by the caller via
   * {@link com.ntros.codec.packet.StatePacket} and {@link com.ntros.codec.PacketCodec}.
   *
   * <p>Never throws: returns a sentinel JSON object on serialization failure so the broadcast
   * stream stays alive.
   */
  @Override
  public byte[] encodeBody(Object data) {
    try {
      return mapper.writeValueAsBytes(data);
    } catch (JsonProcessingException e) {
      return ENCODE_ERROR_BYTES;
    }
  }
}
