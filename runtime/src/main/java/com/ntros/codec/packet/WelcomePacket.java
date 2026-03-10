package com.ntros.codec.packet;

public record WelcomePacket(int playerId) implements Packet {

  public static final byte TYPE = 0x01;

  public byte type() {
    return TYPE;
  }
}
