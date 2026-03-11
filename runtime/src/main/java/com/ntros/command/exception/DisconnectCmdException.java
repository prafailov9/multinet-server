package com.ntros.command.exception;

public class DisconnectCmdException extends RuntimeException {

  public DisconnectCmdException() {
  }

  public DisconnectCmdException(String message) {
    super(message);
  }

  public DisconnectCmdException(String message, Throwable cause) {
    super(message, cause);
  }
}
