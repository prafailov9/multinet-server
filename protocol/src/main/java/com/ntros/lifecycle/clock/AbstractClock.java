package com.ntros.lifecycle.clock;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tick driver with a single-threaded scheduler and a small lifecycle.
 * <p>
 * Responsibilities: - Start/stop/shutdown the schedule. - Pause/resume (scheduled, but tick body is
 * skipped while paused). - Change tick rate and swap the tick task (both trigger a safe
 * reschedule). - Notify an optional listener before/after each tick and maintain a tick counter.
 * <p>
 * Guarantees: - Tick bodies never overlap (executed on a single scheduler thread or a single
 * worker, depending on the concrete clock). - Reschedules are atomic from the caller’s perspective
 * (no overlapping schedules).
 * <p>
 * Policy is provided by subclasses via {@link #schedule(Runnable, long)}: - FixedRateClock   →
 * scheduleAtFixedRate (target cadence; can backlog under load). - FixedDelayClock  →
 * scheduleWithFixedDelay (no backlog; effective TPS drops under load). - PacedRateClock   →
 * fixed-rate intent with a drop-if-busy gate (no backlog; ticks may drop).
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

  // --- Template methods for subclasses --------------------------------------

  /**
   * Implement the scheduling policy (fixed-rate, fixed-delay, or custom).
   */
  protected abstract ScheduledFuture<?> schedule(Runnable lifecycleWrappedTask,
      long intervalMs);

  /**
   * Builds the runnable that enforces the tick lifecycle: pause check → onTickStart → task.run() →
   * onTickEnd.
   */
  protected Runnable wrapWithLifecycle(Runnable task) {
    return () -> {
      if (isPaused()) {
        return;
      }

      long start = System.nanoTime();
      long n = ++tickCount;

      Clock.TickListener l = listener;
      if (l != null) {
        l.onTickStart(n);
      }

      task.run();

      long duration = System.nanoTime() - start;
      if (l != null) {
        l.onTickEnd(n, duration);
      }
    };
  }

  // --- Internals ---

  protected boolean isTicking() {
    return scheduledTaskFuture != null && !scheduledTaskFuture.isCancelled()
        && !scheduledTaskFuture.isDone();
  }

  protected synchronized void scheduleTickTask() {
    if (currentTask == null) {
      return;
    }
    long intervalMs = 1000L / tickRate;
    Runnable wrapper = wrapWithLifecycle(currentTask);
    scheduledTaskFuture = schedule(wrapper, intervalMs);
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
