package com.ntros.model.world;

import java.util.List;

public record Message(CommandType command, List<String> args) {


  @Override
  public String toString() {
    return "Message{" +
        "command=" + command +
        ", args=" + args +
        '}';
  }
}
