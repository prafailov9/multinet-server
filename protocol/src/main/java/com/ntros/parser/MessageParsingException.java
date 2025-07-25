package com.ntros.parser;

public class MessageParsingException extends RuntimeException {

  public MessageParsingException(String errorMessage) {
    super(errorMessage);
  }

  public MessageParsingException(String errorMessage, Throwable cause) {
    super(errorMessage, cause);
  }

}
