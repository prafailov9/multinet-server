package com.ntros.lifecycle.clock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FixedRateClockDeterministicTest {

  // 10ms
  private static final int DEFAULT_TICK_RATE = 100;

  private ManualScheduler scheduler;
  private FixedRateClock clock;

  @BeforeEach
  void setUp() {
    scheduler = new ManualScheduler();

    // clock will tick 100 times per second
    clock = new FixedRateClock(DEFAULT_TICK_RATE, scheduler);
  }

  @AfterEach
  void tearDown() {
    clock.shutdown();
  }

  @Test
  void tick_advanceTime_ticksFireAtFixedRate() {
    var count = new AtomicInteger();
    clock.tick(count::incrementAndGet);

    var expectedTicksCount = new AtomicInteger(11);

    // 0,10,20,...,100  => 11 firings
    scheduler.advanceTimeBy(DEFAULT_TICK_RATE);
    assertThat(count.get()).isEqualTo(expectedTicksCount.get());
  }

  @Test
  void pause_resume_ticksStopThenContinue() {
    var count = new AtomicInteger();
    clock.tick(count::incrementAndGet);

    scheduler.advanceTimeBy(50);            // 6 firings (0..50)
    assertThat(count.get()).isEqualTo(6);

    clock.pause();
    scheduler.advanceTimeBy(50);            // still firing, but wrapper returns early
    assertThat(count.get()).isEqualTo(6);

    clock.resume();
    scheduler.advanceTimeBy(20);            // 2 more (60,70 or 110,120 depending on reschedule)
    assertThat(count.get()).isEqualTo(8);
  }

  @Test
  void updateTask_swapRunnable_newTaskRuns() {
    var a = new AtomicInteger();
    var b = new AtomicInteger();

    clock.tick(a::incrementAndGet);
    scheduler.advanceTimeBy(30);            // 4 firings (0,10,20,30)
    assertThat(a.get()).isEqualTo(4);

    clock.updateTask(b::incrementAndGet);
    a.set(0);
    b.set(0);

    scheduler.advanceTimeBy(40);            // 5 firings
    assertThat(a.get()).isEqualTo(0);
    assertThat(b.get()).isEqualTo(5);
  }

  @Test
  void updateTickRate_increaseFrequency_tickCountRisesFaster() {
    var count = new AtomicInteger();
    clock.tick(count::incrementAndGet);

    scheduler.advanceTimeBy(100);                 // 11 at 100 tps
    int base = count.get();
    assertThat(base).isEqualTo(11);

    clock.updateTickRate(200);                // 5ms
    scheduler.advanceTimeBy(100);                 // 21 firings (0..100 step 5ms => 21)
    assertThat(count.get() - base).isEqualTo(21);
  }

  @Test
  void tick_slowTask_fixedRate_runsAllScheduledFiringsDespiteWorkTime() {
    // Simulate 30ms work per tick w/o real sleeping.
    Runnable slowTask = () -> ManualScheduler.advanceCurrentByMillis(30);

    var count = new java.util.concurrent.atomic.AtomicInteger();
    clock.tick(() -> {
      slowTask.run();
      count.incrementAndGet();
    });

    scheduler.advanceTimeBy(100); // scheduled at 0..100 every 10ms -> 11 firings
    assertThat(count.get()).isEqualTo(11);
  }
}
