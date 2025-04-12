package com.ntros.server.scheduler;

import com.ntros.runtime.Instance;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

@Slf4j
public class WorldTickScheduler {

    private final List<Instance> instances = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final int tickRate;
    private ScheduledFuture<?> tickTask; // reference to the tick task for management;

    public WorldTickScheduler(int tickRate) {
        this.tickRate = tickRate;
    }

    public WorldTickScheduler() {
        this(10);
    }

    public void register(Instance instance) {
        instances.add(instance);
    }

    public synchronized void start() {
        // avoid scheduling multiple tasks if tickTask is already running
        if (isTickTaskRunning()) {
            return;
        }
        long interval = 1000 / tickRate;

        // init the task
        tickTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                for (Instance instance : instances) {
                    instance.run();
                }
            } catch (Exception ex) {
                log.error("Error during world tick: ", ex);
            }
        }, 0, interval, TimeUnit.MILLISECONDS);

    }

    public synchronized void stop() {
        if (tickTask != null) {
            tickTask.cancel(false); // cancel the scheduled task without interrupting if running
            tickTask = null;
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    private boolean isTickTaskRunning() {
        return tickTask != null && !tickTask.isCancelled() && !tickTask.isDone();
    }


}


