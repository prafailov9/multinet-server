package com.ntros.lifecycle.clock;

/**
 * A clock needs a runnable; expose a way to set/replace it.
 */
public interface TickTaskConfig {

  /**
   * Replaces the currently scheduled task with a new one. Useful for dynamic behavior changes.
   *
   * @param task the new task to run on each tick
   */
  void updateTask(Runnable task);
}