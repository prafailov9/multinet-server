package com.ntros.lifecycle.clock;

import org.junit.jupiter.api.Test;

public class DFTests {

  // forks
  private final Object f1 = new Object();
  private final Object f2 = new Object();
  private final Object f3 = new Object();
  private final Object f4 = new Object();
  private final Object f5 = new Object();

  private final Object door = new Object();
  private long nextTicket = 0L;
  private long nowServing = 0L;

//  @Test
  public void dpTestV2() throws InterruptedException {
    Thread t1 = createThread("ph1", createInfTask(f1, f5)); // forks ordered low->high
    Thread t2 = createThread("ph2", createInfTask(f1, f2));
    Thread t3 = createThread("ph3", createInfTask(f2, f3));
    Thread t4 = createThread("ph4", createInfTask(f3, f4));
    Thread t5 = createThread("ph5", createInfTask(f4, f5));

    t1.start();
    t2.start();
    t3.start();
    t4.start();
    t5.start();

    t1.join();
    t2.join();
    t3.join();
    t4.join();
    t5.join();
  }

  private Thread createThread(String name, Runnable task) {
    var t = new Thread(task);
    t.setName(name);
    return t;
  }

  /**
   * Philosophers loop forever:
   * - Take a FIFO ticket and wait for turn (fairness).
   * - Lock forks in global order (deadlock avoidance).
   * - Eat, then release forks and pass the turn.
   */
  private Runnable createInfTask(Object lower, Object higher) {
    return () -> {
      int c = 0;
      while (true) {
        // Fair entrance: only the ticket holder proceeds
        long myTicket = enterTable();
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        try {
          synchronized (lower) {
            synchronized (higher) {
              c++;
//              System.out.printf("%s ate %s %n", Thread.currentThread().getName(), c);
            }
          }
        } finally {
          leaveTable(myTicket);
        }
      }
    };
  }

  private long enterTable() {
    synchronized (door) {
      long ticket = nextTicket++;
      System.out.printf("%s waiting with ticket: %s%n", Thread.currentThread().getName(), ticket);
      while (ticket != nowServing) {
        System.out.printf("Serving: %s...%n", nowServing);
        waitOnLock(door);
      }
      System.out.printf("%s got turn: %s%n", Thread.currentThread().getName(), ticket);
      return ticket;
    }
  }

  private void leaveTable(long myTicket) {
    synchronized (door) {
      // advance to next in strict FIFO order
      nowServing = myTicket + 1;
      door.notifyAll();
    }
  }

  private void waitOnLock(final Object lock) {
    try {
      lock.wait();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
