package com.ntros.connection;

import java.io.IOException;

/**
 * Transport-Level Abstraction. Handles raw communication per session/client.
 */
public interface Connection {

    void send(String data);
    String receive();
    void close();
    boolean isOpen();

}
