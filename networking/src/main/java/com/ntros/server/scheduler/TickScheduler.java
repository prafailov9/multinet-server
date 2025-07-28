package com.ntros.server.scheduler;

public interface TickScheduler {

  void tick(Runnable task);

  // signals to shut down
  void stop();

  void shutdown();
}
