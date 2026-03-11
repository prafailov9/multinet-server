package com.ntros.command.exception;

public class MoveCmdException extends RuntimeException {

  public MoveCmdException() {
  }

  public MoveCmdException(String message) {
    super(message);
  }

  public MoveCmdException(String message, Throwable cause) {
    super(message, cause);
  }
}
