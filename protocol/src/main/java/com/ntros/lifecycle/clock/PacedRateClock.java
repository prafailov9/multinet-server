package com.ntros.lifecycle.clock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FixedRateBackpressureClock: scheduleAtFixedRate + *in-flight gate*.
 * <p>
 * Semantics: - Fires at fixed rate, but if the previous tick hasn't completed yet, the current
 * firing is *skipped* (no new work enqueued). This prevents backlog growth. - Achieves bounded
 * latency and avoids "catch-up bursts" while keeping a fixed-rate intent.
 * <p>
 * Pros: - No backlog; steady output cadence from the perspective of consumers. - Latency remains
 * bounded even under spikes.
 * <p>
 * Cons: - Ticks can be *dropped* under load (effective TPS dips), which is typically fine for
 * real-time UX if inputs are coalesced and/or rendering interpolates.
 * <p>
 * Notes: - This applies backpressure at the clock layer. If you prefer to keep the clock "dumb,"
 * you can implement a similar gate in the actor or the instance instead.
 */
public class PacedRateClock extends AbstractClock {

  private final AtomicBoolean inFlight = new AtomicBoolean(false);
  private final ExecutorService worker;

  public PacedRateClock(int initialTickRate) {
    this(initialTickRate,
        Executors.newSingleThreadScheduledExecutor(r -> {
          var t = new Thread(r, "clock-paced-rate-bp");
          t.setDaemon(true);
          return t;
        }),
        Executors.newSingleThreadExecutor(r -> {
          var t = new Thread(r, "clock-paced-rate-bp-worker");
          t.setDaemon(true);
          return t;
        }));
  }

  PacedRateClock(int initialTickRate,
      java.util.concurrent.ScheduledExecutorService scheduler,
      java.util.concurrent.ExecutorService worker) {
    super(initialTickRate, scheduler);
    this.worker = worker;
  }

  @Override
  protected ScheduledFuture<?> schedule(Runnable lifecycleWrappedTask, long intervalMs) {
    // 'wrapper' already includes pause check, tick count, and listener callbacks
    return scheduler.scheduleAtFixedRate(() -> {
      if (isPaused()) {
        return;
      }
      if (!inFlight.compareAndSet(false, true)) {
        return; // drop this firing
      }
      worker.execute(() -> {
        try {
          lifecycleWrappedTask.run();
        } finally {
          inFlight.set(false);
        }
      });
    }, 0, intervalMs, MILLISECONDS);
  }

  @Override
  public void shutdown() {
    super.shutdown();
    worker.shutdownNow();
  }
}

