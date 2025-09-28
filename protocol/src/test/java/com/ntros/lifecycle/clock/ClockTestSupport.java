package com.ntros.lifecycle.clock;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class ClockTestSupport {

  static final Duration SHORT = Duration.ofMillis(200);
  static final Duration MEDIUM = Duration.ofMillis(400);

  static final class Probe implements Clock.TickListener {

    final List<Long> startsNanos = new CopyOnWriteArrayList<>();
    final List<Long> durationsNanos = new CopyOnWriteArrayList<>();

    @Override
    public void onTickStart(long tickCount) {
      startsNanos.add(System.nanoTime());
    }

    @Override
    public void onTickEnd(long tickCount, long durationNanos) {
      durationsNanos.add(durationNanos);
    }

    long minStartIntervalMillis() {
      if (startsNanos.size() < 2) {
        return Long.MAX_VALUE;
      }
      long min = Long.MAX_VALUE;
      for (int i = 1; i < startsNanos.size(); i++) {
        long dt = (startsNanos.get(i) - startsNanos.get(i - 1)) / 1_000_000L;
        if (dt < min) {
          min = dt;
        }
      }
      return min;
    }
  }

}
