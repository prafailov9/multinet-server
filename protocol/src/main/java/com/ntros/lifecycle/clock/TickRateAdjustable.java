package com.ntros.lifecycle.clock;

/** Clock-specific controls */
public interface TickRateAdjustable {
  void updateTickRate(int ticksPerSecond);
  int getTickRate();
}