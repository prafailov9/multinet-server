package com.ntros.session.process;

public class NoResponseFromServerException extends RuntimeException {

  public NoResponseFromServerException(String msg) {
    super(msg);
  }

}
