package com.ntros.codec.packet;

public record MovePacket(byte direction) implements Packet {

  public static final byte TYPE = 0x02;

  public byte type() {
    return 0x02;
  }
}