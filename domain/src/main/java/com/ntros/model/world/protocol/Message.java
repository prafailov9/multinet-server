package com.ntros.model.world.protocol;

import java.util.List;
import java.util.stream.Collectors;

public record Message(CommandType command, List<String> args) {

  private static final String MESSAGE_PREFIX = "";
  private static final String MESSAGE_SUFFIX = "\n";
  private static final String WHITESPACE_DELIMITER = " ";


  private String joinArguments() {
    return args.stream().collect(Collectors.joining(WHITESPACE_DELIMITER, MESSAGE_PREFIX,
        MESSAGE_SUFFIX));
  }

  @Override
  public String toString() {
    return String.format("%s %s", command, joinArguments());
  }
}
