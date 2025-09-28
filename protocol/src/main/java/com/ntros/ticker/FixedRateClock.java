package com.ntros.ticker;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.ntros.command.impl.AbstractCommand;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Schedules a task at a target rate using scheduleAtFixedRate. The scheduler fires at every
 * interval regardless of how long the previous tick took. If a tick overruns (takes longer than the
 * interval), new firings still occur and will queue up behind the single-thread scheduler/actor,
 * creating a backlog. process every scheduled tick eventually (no drops by the clock itself) Under
 * sustained load, backlogs increase latency and can cause "catch-up bursts" later.
 */
public class FixedRateClock extends AbstractClock {

  public FixedRateClock(int initialTickRate) {
    super(initialTickRate, "clock-fixed-rate");
  }

  FixedRateClock(int initialTickRate, ScheduledExecutorService scheduler) {
    super(initialTickRate, scheduler);
  }

  @Override
  protected ScheduledFuture<?> scheduleTask(Runnable wrapper, long intervalMs) {
    return scheduler.scheduleAtFixedRate(wrapper, 0, intervalMs, MILLISECONDS);
  }

}
