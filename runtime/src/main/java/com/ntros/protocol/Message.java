package com.ntros.protocol;

import static com.ntros.protocol.CommandType.ACK;
import static com.ntros.protocol.CommandType.AUTH_SUCCESS;
import static com.ntros.protocol.CommandType.ERROR;
import static com.ntros.protocol.CommandType.REG_SUCCESS;
import static com.ntros.protocol.CommandType.WELCOME;

import java.util.List;
import java.util.stream.Collectors;

import java.util.Objects;

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

  public static Message welcome(String playerName) {
    return new Message(WELCOME, List.of(playerName));
  }

  public static Message ack(long sessionId) {
    return new Message(ACK, List.of(String.valueOf(sessionId)));

  }

  public static Message authSuccess(long sessionId) {
    return new Message(AUTH_SUCCESS, List.of(String.valueOf(sessionId)));
  }

  public static Message registrationSuccess(long sessionId) {
    return new Message(REG_SUCCESS, List.of(String.valueOf(sessionId)));
  }

  public static Message registrationSuccess(long sessionId, String clientName) {
    return new Message(REG_SUCCESS, List.of(String.valueOf(sessionId), clientName));
  }

  public static Message error(String err) {
    return new Message(ERROR, List.of(err));
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

