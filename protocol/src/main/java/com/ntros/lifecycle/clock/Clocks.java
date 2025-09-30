package com.ntros.lifecycle.clock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Clocks {

  private static final int DEFAULT_TICK_RATE = 70;
  private static final Long PACED_ID = 1L;
  private static final Long FIXED_RATE_ID = 1L;
  private static final Long FIXED_DELAY_ID = 1L;

  private static final Map<Long, Clock> CLOCK_MAP;

  static {
    CLOCK_MAP = new ConcurrentHashMap<>();

    CLOCK_MAP.put(1L, new FixedRateClock(DEFAULT_TICK_RATE));
    CLOCK_MAP.put(2L, new FixedDelayClock(DEFAULT_TICK_RATE));
    CLOCK_MAP.put(3L, new PacedRateClock(DEFAULT_TICK_RATE));

  }

  public static Clock paced(int tps) {
    var c = CLOCK_MAP.get(PACED_ID);
    c.updateTickRate(tps);
    return c;
  }

  public static Clock fixedRate(int tps) {
    return new FixedRateClock(tps);
  }


}
