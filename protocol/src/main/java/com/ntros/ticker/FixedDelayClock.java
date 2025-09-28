package com.ntros.ticker;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledFuture;


/**
 * FixedDelayClock: schedules the *next* tick only after the *previous* finishes,
 * using scheduleWithFixedDelay.
 *
 * Semantics:
 * - There is never any overlap: the delay interval is measured from the *end*
 *   of the previous tick to the *start* of the next.
 * - If the tick takes longer than expected, effective TPS decreases ("time dilation"),
 *   but there is no backlog.
 *
 * Pros:
 * - Stable resource usage; no queue growth or bursts.
 * - Simple mental model; each tick is strictly serialized by design.
 *
 * Cons:
 * - Effective tick rate drops under load (you won't keep the target cadence).
 * - If you require real-time motion fidelity, you may want to pass an actual delta time (dt)
 *   into your simulation step to compensate.
 */
public class FixedDelayClock extends AbstractClock {

  public FixedDelayClock(int initialTickRate) {
    super(initialTickRate, "clock-fixed-delay");
  }

  @Override
  protected ScheduledFuture<?> scheduleInternal(Runnable wrapper, long intervalMs) {
    return scheduler.scheduleWithFixedDelay(wrapper, 0, intervalMs, MILLISECONDS);
  }
}