package com.ntros.command.exception;

public class AuthCmdException extends RuntimeException {

  public AuthCmdException() {
  }

  public AuthCmdException(String message) {
    super(message);
  }

  public AuthCmdException(String message, Throwable cause) {
    super(message, cause);
  }
}
