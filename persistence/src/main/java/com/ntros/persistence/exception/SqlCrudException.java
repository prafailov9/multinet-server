package com.ntros.persistence.exception;

public class SqlCrudException extends RuntimeException {

  public SqlCrudException() {
  }

  public SqlCrudException(String message) {
    super(message);
  }

  public SqlCrudException(String message, Throwable cause) {
    super(message, cause);
  }
}
