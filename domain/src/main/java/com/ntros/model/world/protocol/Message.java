package com.ntros.model.world.protocol;

import java.util.List;
import java.util.stream.Collectors;

public record Message(CommandType commandType, List<String> args) {

  private static final String MESSAGE_PREFIX = "";
  private static final String MESSAGE_SUFFIX = "\n";
  private static final String WHITESPACE_DELIMITER = " ";

  public Message(CommandType commandType, List<String> args) {
    this.commandType = commandType;
    if (args == null || args.isEmpty()) {
      throw new IllegalArgumentException("Argument List cannot be empty.");
    }
    this.args = args;
  }

  private String joinArguments() {
    return args.stream().collect(Collectors.joining(WHITESPACE_DELIMITER, MESSAGE_PREFIX,
        MESSAGE_SUFFIX));
  }

  @Override
  public String toString() {
    return String.format("%s %s", commandType, joinArguments());
  }
}
