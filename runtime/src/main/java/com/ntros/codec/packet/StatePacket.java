package com.ntros.codec.packet;

import java.util.Arrays;
import java.util.Objects;

/**
 * STATE (0x03) — server→client world snapshot push.
 *
 * <p>Payload layout (inside the binary frame):
 * <pre>
 *   [ seq:8 ][ worldNameLen:1 ][ worldName:N ][ body:M ]
 * </pre>
 *
 * <ul>
 *   <li>{@code seq}          — monotonically increasing sequence number; lets the client detect
 *                              dropped frames and apply delta logic.</li>
 *   <li>{@code worldName}    — identifies the source world (max 255 UTF-8 bytes).</li>
 *   <li>{@code body}         — serialized world snapshot; JSON for now, format is opaque to the
 *                              codec layer.</li>
 * </ul>
 *
 * <p>Note: {@link #equals} and {@link #hashCode} are overridden because Java records use reference
 * equality for {@code byte[]} by default.
 */
public record StatePacket(long seq, String worldName, byte[] body) implements Packet {

  public static final byte TYPE = 0x03;

  /** Max world-name length that fits in a single unsigned byte length prefix. */
  private static final int MAX_WORLD_NAME_BYTES = 255;

  public StatePacket {
    Objects.requireNonNull(worldName, "worldName must not be null");
    Objects.requireNonNull(body, "body must not be null");
    if (worldName.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_WORLD_NAME_BYTES) {
      throw new IllegalArgumentException(
          "worldName exceeds " + MAX_WORLD_NAME_BYTES + " UTF-8 bytes");
    }
    body = Arrays.copyOf(body, body.length); // defensive copy — records are not truly immutable for arrays
  }

  @Override
  public byte type() {
    return TYPE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StatePacket other)) return false;
    return seq == other.seq
        && worldName.equals(other.worldName)
        && Arrays.equals(body, other.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(seq, worldName, Arrays.hashCode(body));
  }

  @Override
  public String toString() {
    return "StatePacket[seq=" + seq
        + ", world=" + worldName
        + ", bodyLen=" + body.length + "]";
  }
}
