package com.ntros.lifecycle.clock;

public interface Clock extends TickRateAdjustable, TickTaskConfig {

  /**
   * Starts the ticker and schedules the given task. No-op if already running.
   *
   * @param task the logic to run on each tick
   */
  void tick(Runnable task);

  /**
   * Stops the ticker without shutting down the scheduler. Can be restarted later.
   */
  void stop();

  /**
   * Permanently shuts down the ticker and underlying resources. Cannot be restarted afterward.
   */
  void shutdown();

  /**
   * Pauses the ticker. The scheduled task is temporarily halted but not discarded.
   */
  void pause();

  /**
   * Resumes the ticker if it was paused.
   */
  void resume();

  /**
   * Returns whether the ticker is currently paused.
   */
  boolean isPaused();

  /**
   * Registers a listener to be notified at the start and end of each tick. Can be used for logging,
   * metrics, or profiling.
   *
   * @param listener a consumer that accepts tick information
   */
  void setListener(TickListener listener);

  /**
   * TickListener interface for observing tick lifecycle events.
   */
  interface TickListener {

    void onTickStart(long tickCount);

    void onTickEnd(long tickCount, long durationNanos);
  }
}
