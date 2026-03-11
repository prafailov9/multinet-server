package com.ntros.command.exception;

public class RegisterCmdException extends RuntimeException {

  public RegisterCmdException() {
  }

  public RegisterCmdException(String message) {
    super(message);
  }

  public RegisterCmdException(String message, Throwable cause) {
    super(message, cause);
  }
}
