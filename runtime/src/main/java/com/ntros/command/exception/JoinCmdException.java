package com.ntros.command.exception;

public class JoinCmdException extends RuntimeException {

  public JoinCmdException() {
  }

  public JoinCmdException(String message) {
    super(message);
  }

  public JoinCmdException(String message, Throwable cause) {
    super(message, cause);
  }
}
