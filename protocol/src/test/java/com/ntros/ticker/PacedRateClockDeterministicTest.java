package com.ntros.ticker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PacedRateClockDeterministicTest {

  ManualScheduler scheduler;
  ManualWorker worker;
  PacedRateClock clock;

  @BeforeEach
  void setUp() {
    scheduler = new ManualScheduler();
    worker = new ManualWorker();
    clock = new PacedRateClock(100, scheduler, worker); // 10ms cadence
  }

  @AfterEach
  void tearDown() {
    clock.shutdown();
  }

  @Test
  void tick_slowTask_skipsWhileBusy_dropsScheduledFirings() {
    var executed = new java.util.concurrent.atomic.AtomicInteger();

    // slow tick body: consumes 30ms of scheduler time deterministically
    Runnable slowTick = () -> {
      ManualScheduler.advanceCurrentByMillis(30); // simulate work time
      executed.incrementAndGet();
    };

    clock.tick(slowTick);

    // Advance 100ms of "wall time". Scheduler fires at 0,10,20,...,100 (11 firings),
    // but only the first enqueues work; the rest are dropped (inFlight=true).
    scheduler.advanceTimeBy(100);
    assertThat(worker.pending()).isEqualTo(1);
    assertThat(executed.get()).isEqualTo(0);

    // Complete that one tick.
    worker.runNext();
    assertThat(executed.get()).isEqualTo(1);

    // Move another 100ms; again, only one tick gets enqueued (others dropped).
    scheduler.advanceTimeBy(100);
    assertThat(worker.pending()).isEqualTo(1);
    worker.runNext();
    assertThat(executed.get()).isEqualTo(2);
  }
}
