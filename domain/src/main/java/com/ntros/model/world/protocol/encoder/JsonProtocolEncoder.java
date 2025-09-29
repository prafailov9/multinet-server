package com.ntros.model.world.protocol.encoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonProtocolEncoder implements ProtocolEncoder {

  private final ObjectMapper mapper = new ObjectMapper();


  @Override
  public String encodeState(StateFrame frame) {
    try {
      String payload = mapper.writeValueAsString(frame);
      return "STATE " + payload;   // single line, newline added by SocketConnection
    } catch (JsonProcessingException e) {
      // fallback so we never break the stream
      return "STATE {\"proto\":" + frame.proto() + ",\"inst\":\"" + frame.inst()
          + "\",\"seq\":" + frame.seq() + ",\"data\":\"_encode_error_\"}";
    }
  }
}
