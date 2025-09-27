package com.ntros.connection;


import java.io.IOException;

/**
 * Transport-Level Abstraction. Handles raw communication per session/client.
 */
public interface Connection {

  /**
   * sends a line + '\n'
   *
   * @param message
   */
  void send(String message);

  /**
   * reads one line (until '\n')
   *
   * @return
   */
  String receive();

  /**
   * Writes: headerLine+ '\n' + body + '\n'
   *
   * @param headerLine
   * @param body
   */
  void sendFrame(String headerLine, byte[] body);

  /**
   * reads exactly N bytes
   *
   * @param length
   * @return
   * @throws IOException
   */
  byte[] receiveBytesExactly(int length) throws IOException;

  void close();

  boolean isOpen();

}
