package com.ntros.connection;


/**
 * Transport-Level Abstraction. Handles raw communication per session/client.
 */
public interface Connection {

  void send(String message);

  String receive();

  void close();

  boolean isOpen();

}
