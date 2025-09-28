package com.ntros.ticker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// 5 second timeout safety net per test
@Timeout(5)
class FixedRateClockTest {

  FixedRateClock clock;

  @BeforeEach
  void setUp() {
    clock = new FixedRateClock(100);
  } // 10ms

  @AfterEach
  void tearDown() {
    clock.shutdown();
  }

  @Test
  void tick_startScheduling_listenerReceivesCallbacks() {
    var runs = new AtomicInteger();
    var probe = new ClockTestSupport.Probe();
    clock.setListener(probe);

    clock.tick(runs::incrementAndGet);

    await().atMost(ClockTestSupport.SHORT)
        .untilAsserted(() -> assertThat(runs.get()).isGreaterThanOrEqualTo(5));
    assertThat(probe.startsNanos).isNotEmpty();
    assertThat(probe.durationsNanos).hasSameSizeAs(probe.startsNanos);
  }

  @Test
  void pause_resume_ticksStopThenContinue() throws Exception {
    var runs = new AtomicInteger();
    clock.tick(runs::incrementAndGet);
    await().atMost(ClockTestSupport.SHORT)
        .untilAsserted(() -> assertThat(runs.get()).isGreaterThan(0));

    clock.pause();
    int before = runs.get();
    TimeUnit.MILLISECONDS.sleep(60);
    assertThat(runs.get()).isEqualTo(before);

    clock.resume();
    await().atMost(ClockTestSupport.SHORT)
        .untilAsserted(() -> assertThat(runs.get()).isGreaterThan(before));
  }

  @Test
  void updateTask_swapRunnable_newTaskRuns() {
    var a = new AtomicInteger();
    var b = new AtomicInteger();

    clock.tick(a::incrementAndGet);
    await().atMost(ClockTestSupport.SHORT)
        .untilAsserted(() -> assertThat(a.get()).isGreaterThan(0));

    clock.updateTask(b::incrementAndGet);
    a.set(0);
    b.set(0);

    await().atMost(ClockTestSupport.SHORT)
        .untilAsserted(() -> {
          assertThat(a.get()).isEqualTo(0);
          assertThat(b.get()).isGreaterThan(0);
        });
  }

  @Test
  void updateTickRate_increaseFrequency_tickCountRisesFaster() throws Exception {
    var runs = new AtomicInteger();
    clock.updateTickRate(20); // 50ms
    clock.tick(runs::incrementAndGet);
    TimeUnit.MILLISECONDS.sleep(160);
    int slow = runs.get();

    runs.set(0);
    clock.updateTickRate(200); // 5ms
    TimeUnit.MILLISECONDS.sleep(160);
    int fast = runs.get();

    assertThat(fast).isGreaterThan(slow);
  }

  @Test
  void tick_slowTask_fixedDelay_includesDelayAfterTask() {
    var clock = new FixedDelayClock(100); // 10ms delay
    try {
      Runnable slowTask = () -> {
        try {
          TimeUnit.MILLISECONDS.sleep(30);
        } catch (InterruptedException ignored) {
        }
      };
      var probe = new ClockTestSupport.Probe();
      clock.setListener(probe);

      clock.tick(slowTask);

      await().atMost(ClockTestSupport.MEDIUM)
          .untilAsserted(() -> assertThat(probe.startsNanos.size()).isGreaterThanOrEqualTo(3));

      long minMs = probe.minStartIntervalMillis();

      // ≈ task(30) + delay(10) = 40ms, allow jitter.
      assertThat(minMs).isGreaterThanOrEqualTo(35L);
    } finally {
      clock.shutdown();
    }
  }

}