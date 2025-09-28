package com.ntros.ticker;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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

  public PacedRateClock(int initialTickRate) {
    super(initialTickRate, "clock-fixed-rate-bp");
  }

  @Override
  protected ScheduledFuture<?> scheduleInternal(Runnable wrapper, long intervalMs) {
    return scheduler.scheduleAtFixedRate(wrapper, 0, intervalMs, MILLISECONDS);
  }

  @Override
  protected Runnable buildWrapper(Runnable task) {
    // Wrap the base wrapper with a simple "skip if busy" gate.
    Runnable base = super.buildWrapper(task);
    return () -> {
      if (!inFlight.compareAndSet(false, true)) {
        // Previous tick hasn't released yet — skip this firing to avoid backlog.
        return;
      }
      try {
        base.run();
      } finally {
        inFlight.set(false);
      }
    };
  }
}
