package com.ntros.ticker;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorldTicker implements Ticker {

  private volatile int tickRate;

  // single-threaded scheduler to ensure sequential, isolated ticks
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  // reference to the tick task for management
  private ScheduledFuture<?> scheduledTaskFuture;

  // The task to run on each tick
  private Runnable currentTask;

  // Indicates whether the ticker is paused
  private final AtomicBoolean paused = new AtomicBoolean(false);

  // Optional listener to observe tick start and end events
  private TickListener listener;

  // Tick counter (number of completed ticks)
  private long tickCount = 0;

  public WorldTicker(int initialTickRate) {
    this.tickRate = initialTickRate;
  }

  /**
   * Starts ticker with provided task if not already running
   *
   * @param task the logic to run on each tick
   */
  @Override
  public synchronized void tick(Runnable task) {
    // avoid scheduling multiple tasks if tickTask is already running
    if (isTicking()) {
      return;
    }
    this.currentTask = task;
    long interval = 1000 / tickRate;

    // init the task
    scheduledTaskFuture = scheduler.scheduleAtFixedRate(task, 0, interval, MILLISECONDS);
  }

  /**
   * Stops ticking without shutting down the scheduler. Can be restarted with tick();
   */
  @Override
  public synchronized void stop() {
    if (scheduledTaskFuture != null) {
      scheduledTaskFuture.cancel(false); // cancel the scheduled task without interrupting if running
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
    if (this.tickRate == ticksPerSecond) {
      return;
    }
    this.tickRate = ticksPerSecond;
    if (isTicking()) {
      reschedule();
    }
  }

  @Override
  public int getTickRate() {
    return tickRate;
  }

  /**
   * Pauses the ticker (task will still be scheduled but will skip execution while paused).
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
  public void setListener(TickListener listener) {
    this.listener = listener;
  }

  @Override
  public boolean isPaused() {
    return paused.get();
  }

  private boolean isTicking() {
    return scheduledTaskFuture
        != null && !scheduledTaskFuture.isCancelled() && !scheduledTaskFuture.isDone();
  }

  private void scheduleTickTask() {
    long interval = 1000L / tickRate; // convert ticks/sec to milliseconds
    scheduledTaskFuture = scheduler.scheduleAtFixedRate(() -> {
      if (isPaused()) {
        return;
      }
      long start = System.nanoTime();
      long currentTick = ++tickCount;

      if (listener != null) {
        listener.onTickStart(currentTick);
      }

      currentTask.run();
      long duration = System.nanoTime() - start;
      if (listener != null) {
        listener.onTickEnd(currentTick, duration);
      }
    }, 0, interval, MILLISECONDS);
  }

  /**
   * Stops and immediately restarts the tick task with updated rate or logic.
   */
  private void reschedule() {
    stop();
    if (currentTask != null && !paused.get()) {
      scheduleTickTask();
    }
  }

}
