package com.ntros.lifecycle.clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
class FixedDelayClockTest {

  FixedDelayClock clock;

  @BeforeEach
  void setUp() { clock = new FixedDelayClock(100); } // 10ms delay
  @AfterEach
  void tearDown() { clock.shutdown(); }

  @Test
  void runs_and_respects_delay_between_ticks() {
    var probe = new ClockTestSupport.Probe();
    clock.setListener(probe);

    // Make each tick heavy (~30ms). With fixed *delay*, start-to-start intervals should be ~ task(30) + delay(10) ~= 40ms.
    clock.tick(() -> {
      try { TimeUnit.MILLISECONDS.sleep(30); } catch (InterruptedException ignored) {}
    });

    await().atMost(ClockTestSupport.MEDIUM)
        .untilAsserted(() -> assertThat(probe.startsNanos.size()).isGreaterThanOrEqualTo(3));

    long minMs = probe.minStartIntervalMillis();
    // Should *not* see sub-5ms bursty intervals; should be >= ~35ms in practice. Use a generous floor for CI jitter.
    assertThat(minMs).isGreaterThanOrEqualTo(20);
  }

  @Test
  void updateTickRate_changes_delay_effectively() throws Exception {
    var probe = new ClockTestSupport.Probe();
    clock.setListener(probe);

    clock.updateTickRate(20); // 50ms delay
    clock.tick(() -> {});
    TimeUnit.MILLISECONDS.sleep(220);
    int slow = probe.startsNanos.size();

    probe.startsNanos.clear();
    clock.updateTickRate(200); // 5ms delay
    TimeUnit.MILLISECONDS.sleep(220);
    int fast = probe.startsNanos.size();

    assertThat(fast).isGreaterThan(slow);
  }
}