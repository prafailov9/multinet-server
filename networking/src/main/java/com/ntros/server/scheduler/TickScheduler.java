package com.ntros.server.scheduler;

public interface TickScheduler {

  void tick(Runnable task);

  void tick();

  // signals to shutdown
  void stop();

  void shutdown();
}
