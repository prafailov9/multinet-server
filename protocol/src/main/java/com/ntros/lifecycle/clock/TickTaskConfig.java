package com.ntros.lifecycle.clock;

/**
 * A clock needs a runnable; expose a way to set/replace it.
 */
public interface TickTaskConfig {

  void updateTask(Runnable task);
}