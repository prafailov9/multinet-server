package com.ntros.server;


import java.io.IOException;

public interface Server {

  void start(int port) throws IOException;

  void stop() throws IOException;

}
