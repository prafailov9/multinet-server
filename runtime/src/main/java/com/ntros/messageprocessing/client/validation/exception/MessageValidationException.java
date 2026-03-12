package com.ntros.messageprocessing.client.validation.exception;

public class MessageValidationException extends RuntimeException {


  public MessageValidationException() {
  }

  public MessageValidationException(String message) {
    super(message);
  }

  public MessageValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
