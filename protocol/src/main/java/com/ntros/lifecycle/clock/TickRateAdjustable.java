package com.ntros.lifecycle.clock;

public interface TickRateAdjustable {

  /**
   * Reschedules the ticker to use a new tick rate. If the ticker is running, it will restart with
   * the updated rate.
   *
   * @param ticksPerSecond the new tick rate
   */
  void updateTickRate(int ticksPerSecond);

  /**
   * Returns the current tick rate (ticks per second).
   */
  int getTickRate();
}