package com.ntros.model.world.protocol;

import java.util.List;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record Message(CommandType commandType, List<String> args) {

  private static final String ARG_DELIMITER = " ";
  private static final String MESSAGE_PREFIX = "";
  private static final String MESSAGE_SUFFIX = "\n";

  public Message {
    Objects.requireNonNull(commandType, "commandType must not be null");
    Objects.requireNonNull(args, "args must not be null");
    if (args.isEmpty()) {
      throw new IllegalArgumentException("Argument list cannot be empty.");
    }
    args = List.copyOf(args); // ensure immutability
  }

  /**
   * Returns the raw protocol string to send over the wire. Example: "WELCOME player1 world1\n"
   */
  public String toWireFormat() {
    return commandType.name() + ARG_DELIMITER + joinArguments();
  }

  /**
   * Joins all arguments into a single space-delimited string, with protocol prefix/suffix applied.
   */
  private String joinArguments() {
    return args.stream()
        .collect(Collectors.joining(ARG_DELIMITER, MESSAGE_PREFIX, MESSAGE_SUFFIX));
  }

  /**
   * For debugging/logging — doesn’t include protocol suffix.
   */
  @Override
  public String toString() {
    return commandType + " " + String.join(ARG_DELIMITER, args);
  }
}

