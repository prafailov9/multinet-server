package com.ntros.server.scheduler;

import com.ntros.runtime.Instance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorldTickScheduler {

    private final List<Instance> instances = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final int tickRate;

    public WorldTickScheduler(int tickRate) {
        this.tickRate = tickRate;
    }

    public WorldTickScheduler() {
        this(10);
    }

    public void register(Instance instance) {
        instances.add(instance);
    }

    public void start() {
        long interval = 3000 / tickRate;

        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (Instance instance : instances) {
                    instance.run();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 0, interval, TimeUnit.MILLISECONDS);

    }

    public void stop() {
        scheduler.shutdownNow();
    }
}


