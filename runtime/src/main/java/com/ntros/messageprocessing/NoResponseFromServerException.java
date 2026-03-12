package com.ntros.messageprocessing;

public class NoResponseFromServerException extends RuntimeException {

  public NoResponseFromServerException(String msg) {
    super(msg);
  }

}
