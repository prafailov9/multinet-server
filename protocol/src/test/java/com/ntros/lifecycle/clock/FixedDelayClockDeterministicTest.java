package com.ntros.lifecycle.clock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FixedDelayClockDeterministicTest {

  ManualScheduler scheduler;
  FixedDelayClock clock;

  @BeforeEach
  void setUp() {
    scheduler = new ManualScheduler();
    clock = new FixedDelayClock(100, scheduler); // 10ms delay after each run
  }

  @AfterEach
  void tearDown() {
    clock.shutdown();
  }

  @Test
  void tick_slowTask_intervalsIncludeTaskDurationNoBacklog() {
    Runnable slow = () -> ManualScheduler.advanceCurrentByMillis(30);

    var count = new AtomicInteger();
    clock.tick(() -> {
      slow.run();
      count.incrementAndGet();
    });

    scheduler.advanceTimeBy(100);
    // With fixed-delay: start-to-start ≈ task(30) + delay(10) = 40ms
    // Fired at ~0, 40, 80 => 3 firings within 100ms window.
    assertThat(count.get()).isEqualTo(3);
  }

  @Test
  void updateTickRate_changeDelay_affectsObservedFrequency() {
    var count = new java.util.concurrent.atomic.AtomicInteger();
    clock.updateTickRate(20); // 50ms delay
    clock.tick(count::incrementAndGet);
    scheduler.advanceTimeBy(200); // 0,50,100,150,200 => 5 runs
    assertThat(count.get()).isEqualTo(5);

    count.set(0);
    clock.updateTickRate(200); // 5ms delay
    scheduler.advanceTimeBy(50);   // 0..50 step 5ms => 11 runs
    assertThat(count.get()).isEqualTo(11);
  }
}
