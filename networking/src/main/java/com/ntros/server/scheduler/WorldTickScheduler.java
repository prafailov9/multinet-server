package com.ntros.server.scheduler;

import com.ntros.runtime.Runtime;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorldTickScheduler {

    private final List<Runtime> runtimes = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final int tickRate;

    public WorldTickScheduler(int tickRate) {
        this.tickRate = tickRate;
    }

    public void register(Runtime runtime) {
        System.out.println("[WorldTickScheduler] Registered runtime: " + runtime.getWorldName());
        runtimes.add(runtime);
    }

    public void start() {
        System.out.println("[WorldTickScheduler] Scheduler thread starting...");
        long interval = 3000 / tickRate;

        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("[WorldTickScheduler] Tick executing. Runtime count: " + runtimes.size());
                for (Runtime runtime : runtimes) {
                    runtime.run();
                }
            } catch (Exception ex) {
                System.err.println("[WorldTickScheduler] ERROR during tick: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, interval, TimeUnit.MILLISECONDS);

    }

    public void stop() {
        scheduler.shutdownNow();
    }
}


