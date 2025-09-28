package com.ntros.ticker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AbstractClock centralizes all common clock mechanics:
 * <p>
 * - Single-threaded scheduler so tick tasks never run concurrently on multiple threads. -
 * Start/stop/shutdown controls. - Pause/resume support (ticks are scheduled but their bodies can be
 * skipped). - Tick listener callbacks (onTickStart/onTickEnd) and tick counting. - Dynamic
 * tick-rate updates and task replacement via a unified (re)scheduling path.
 * <p>
 * Subclasses only decide *how* to schedule: - FixedRateClock: scheduleAtFixedRate (target cadence,
 * can overlap intent => backlog if slow). - FixedDelayClock: scheduleWithFixedDelay (no overlap by
 * design; adaptive TPS / time dilation). - FixedRateBackpressureClock: fixed-rate intent + a gate
 * to skip ticks if prior hasn't finished (no backlog, bounded latency; ticks may be dropped).
 */
public abstract class AbstractClock implements Clock {

  // single-threaded scheduler to ensure sequential, isolated ticks
  protected final ScheduledExecutorService scheduler;

  // The task to run on each tick
  protected Runnable currentTask;

  private volatile int tickRate;
  private final AtomicBoolean paused = new AtomicBoolean(false);
  private Clock.TickListener listener;

  // listener to observe tick start and end events
  private long tickCount = 0;

  // reference to the tick task for management
  private ScheduledFuture<?> scheduledTaskFuture;

  protected AbstractClock(int initialTickRate, String threadName) {
    this.tickRate = Math.max(1, initialTickRate);
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, threadName);
      t.setDaemon(true);
      return t;
    });
  }

  // inject scheduler for testing
  protected AbstractClock(int initialTickRate, ScheduledExecutorService scheduler) {
    this.tickRate = Math.max(1, initialTickRate);
    this.scheduler = scheduler;
  }

  /// --- Public API ---

  /**
   * Starts ticker with provided task if not already running
   *
   * @param task the logic to run on each tick
   */
  @Override
  public synchronized void tick(Runnable task) {
    if (isTicking()) {
      return;
    }
    this.currentTask = task;
    scheduleTickTask();
  }

  /**
   * Stops ticking without shutting down the scheduler. Can be restarted with tick();
   */
  @Override
  public synchronized void stop() {
    if (scheduledTaskFuture != null) {
      // cancel the scheduled task without interrupting if running
      scheduledTaskFuture.cancel(false);
      scheduledTaskFuture = null;
    }
  }

  /**
   * Shuts down scheduler. Cannot be restarted.
   */
  @Override
  public void shutdown() {
    stop();
    scheduler.shutdownNow();
  }


  /**
   * Updates the tick rate dynamically and reschedules the task if running.
   */
  @Override
  public void updateTickRate(int ticksPerSecond) {
    int tps = Math.max(1, ticksPerSecond);
    if (this.tickRate == tps) {
      return;
    }
    this.tickRate = tps;
    if (isTicking()) {
      reschedule();
    }
  }

  @Override
  public int getTickRate() {
    return tickRate;
  }

  /**
   * Pauses the clock (task will still be scheduled but will skip execution while paused).
   */
  @Override
  public void pause() {
    paused.set(true);
  }

  /**
   * Resumes ticking if paused.
   */
  @Override
  public void resume() {
    paused.set(false);
    if (!isTicking() && currentTask != null) {
      scheduleTickTask();
    }
  }

  /**
   * Replaces the current task. Will reschedule if already ticking
   *
   * @param task the new task to run on each tick
   */
  @Override
  public void updateTask(Runnable task) {
    this.currentTask = task;
    if (isTicking()) {
      reschedule();
    }
  }

  @Override
  public void setListener(Clock.TickListener listener) {
    this.listener = listener;
  }

  @Override
  public boolean isPaused() {
    return paused.get();
  }

  // ---- Template methods for subclasses -------------------------------------

  /**
   * fixed-rate or fixed-delay scheduling.
   */
  protected abstract ScheduledFuture<?> scheduleInternal(Runnable wrapper, long intervalMs);

  /**
   * override to add gating/backpressure.
   */
  protected Runnable buildWrapper(Runnable task) {
    return () -> {
      if (isPaused()) {
        return;
      }

      long start = System.nanoTime();
      long n = ++tickCount;

      if (listener != null) {
        listener.onTickStart(n);
      }

      task.run();

      long duration = System.nanoTime() - start;
      if (listener != null) {
        listener.onTickEnd(n, duration);
      }
    };
  }

  // ---- Internals ------------------------------------------------------------

  protected boolean isTicking() {
    return scheduledTaskFuture != null && !scheduledTaskFuture.isCancelled()
        && !scheduledTaskFuture.isDone();
  }

  protected synchronized void scheduleTickTask() {
    if (currentTask == null) {
      return;
    }
    long interval = 1000L / tickRate;
    Runnable wrapper = buildWrapper(currentTask);
    scheduledTaskFuture = scheduleInternal(wrapper, interval);
  }

  /**
   * Stops and immediately restarts the tick task with updated rate or logic.
   */
  protected synchronized void reschedule() {
    stop();
    if (currentTask != null && !paused.get()) {
      scheduleTickTask();
    }
  }

}
