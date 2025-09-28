package com.ntros.ticker;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;


/**
 * FixedDelayClock: schedules the *next* tick only after the *previous* finishes, using
 * scheduleWithFixedDelay.
 * <p>
 * Semantics: - There is never any overlap: the delay interval is measured from the *end* of the
 * previous tick to the *start* of the next. - If the tick takes longer than expected, effective TPS
 * decreases ("time dilation"), but there is no backlog.
 * <p>
 * Pros: - Stable resource usage; no queue growth or bursts. - Simple mental model; each tick is
 * strictly serialized by design.
 * <p>
 * Cons: - Effective tick rate drops under load (you won't keep the target cadence). - If you
 * require real-time motion fidelity, you may want to pass an actual delta time (dt) into your
 * simulation step to compensate.
 */
public class FixedDelayClock extends AbstractClock {

  public FixedDelayClock(int initialTickRate) {
    super(initialTickRate, "clock-fixed-delay");
  }

  FixedDelayClock(int initialTickRate, ScheduledExecutorService scheduler) {
    super(initialTickRate, scheduler);
  }

  @Override
  protected ScheduledFuture<?> scheduleInternal(Runnable wrapper, long intervalMs) {
    return scheduler.scheduleWithFixedDelay(wrapper, 0, intervalMs, MILLISECONDS);
  }
}