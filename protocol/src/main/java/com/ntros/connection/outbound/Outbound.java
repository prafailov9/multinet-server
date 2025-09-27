package com.ntros.connection.outbound;

sealed interface Outbound permits Outbound.Line, Outbound.Frame {
  record Line(String text) implements Outbound {}
  record Frame(String header, byte[] body) implements Outbound {}
}