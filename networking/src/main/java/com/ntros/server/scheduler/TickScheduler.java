package com.ntros.server.scheduler;

public interface TickScheduler {
    void start();
    void tick();
    void stop();
}
