package com.ntros.server.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServerTickScheduler implements TickScheduler {

    private final int tickRate;
    private ScheduledFuture<?> tickTask; // reference to the tick task for management;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    public ServerTickScheduler(int tickRate) {
        this.tickRate = tickRate;
    }

    @Override
    public synchronized void tick(Runnable task) {
        // avoid scheduling multiple tasks if tickTask is already running
        if (isTickTaskRunning()) {
            return;
        }
        long interval = 1000 / tickRate;

        // init the task
        tickTask = scheduler.scheduleAtFixedRate(task, 0, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void tick() {

    }

    @Override
    public synchronized void stop() {
        if (tickTask != null) {
            tickTask.cancel(false); // cancel the scheduled task without interrupting if running
            tickTask = null;
        }
    }

    @Override
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private boolean isTickTaskRunning() {
        return tickTask != null && !tickTask.isCancelled() && !tickTask.isDone();
    }

}
