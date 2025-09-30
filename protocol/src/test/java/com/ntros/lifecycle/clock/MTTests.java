package com.ntros.lifecycle.clock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MTTests {

  private int c = 0;
  private final Object cLock = new Object();
  private int ops = 0;
  private final Object opsLock = new Object();

  private static final int CAP = 3;
  private final ExecutorService producer = Executors.newSingleThreadExecutor();
  private final ExecutorService consumer = Executors.newSingleThreadExecutor();
  private final List<Integer> sharedData = new ArrayList<>(CAP);

  @BeforeEach
  public void setup() {
  }

  @AfterEach
  public void tearDown() {
    producer.shutdownNow();
    consumer.shutdownNow();
  }

  @Test
  public void prodConsProblem() {
    while (ops < 300) {
      producer.execute(produce());
      consumer.execute(consume());
    }

  }

  private Runnable produce() {
    return () -> {
      while (sharedData.size() < CAP) {
        inc();
        System.out.printf("<P>: adding %s...%n", c);
        sharedData.add(c);
      }
      incOp();
    };
  }

  private Runnable consume() {
    return () -> {
      while (!sharedData.isEmpty()) {
        System.out.printf("<C>: removing %s...%n", c);
        c = sharedData.removeFirst();
      }
    };
  }


  private void inc() {
    synchronized (cLock) {
      c++;
    }
  }

  private void dec() {
    synchronized (cLock) {
      c--;
    }
  }

  private void incOp() {
    synchronized (cLock) {
      ops++;
    }
  }


}
