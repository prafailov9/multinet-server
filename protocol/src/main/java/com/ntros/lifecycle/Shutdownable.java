package com.ntros.lifecycle;

public interface Shutdownable {

  void shutdown();             // Final stop; releases threads/sockets permanently.

}
