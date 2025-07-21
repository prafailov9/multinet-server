package com.ntros.server.scheduler;

public interface TickScheduler {
    void tick(Runnable task);
    void stop();
}
