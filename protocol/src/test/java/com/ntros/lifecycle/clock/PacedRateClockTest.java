package com.ntros.lifecycle.clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;

@Timeout(5)
class PacedRateClockTest {

  PacedRateClock clock;

  @BeforeEach
  void setUp() {
    clock = new PacedRateClock(100);
  }

  @AfterEach
  void tearDown() {
    clock.shutdown();
  }

  @Test
  void skips_ticks_while_busy_no_bursts() {
    var probe = new ClockTestSupport.Probe();
    clock.setListener(probe);

    // 30ms task > 10ms period; while in-flight, firings are SKIPPED due to the gate.
    clock.tick(() -> {
      try {
        TimeUnit.MILLISECONDS.sleep(30);
      } catch (InterruptedException ignored) {
      }
    });

    await().atMost(ClockTestSupport.MEDIUM)
        .untilAsserted(() -> assertThat(probe.startsNanos.size()).isGreaterThanOrEqualTo(3));

    long minMs = probe.minStartIntervalMillis();

    // Key property vs FixedRate: no ~0ms catch-up bursts; min interval >= about the period.
    // Allow some jitter; assert >= 6ms rather than strict 10ms to be CI-friendly.
    assertThat(minMs).isGreaterThanOrEqualTo(6);
  }
}
