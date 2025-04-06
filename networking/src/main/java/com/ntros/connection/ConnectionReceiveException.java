package com.ntros.connection;

public class ConnectionReceiveException extends RuntimeException {

    public ConnectionReceiveException(String message) {
        super(message);
    }

    public ConnectionReceiveException(String message, Throwable cause) {
        super(message, cause);
    }

}
