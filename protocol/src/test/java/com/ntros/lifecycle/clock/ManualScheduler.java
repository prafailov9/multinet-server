package com.ntros.lifecycle.clock;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManualScheduler implements ScheduledExecutorService {

  // Access from a scheduled task (to simulate "work time" passing)
  private static final ThreadLocal<ManualScheduler> CURRENT = new ThreadLocal<>();

  public static ManualScheduler current() {
    return CURRENT.get();
  }

  public static void advanceCurrentByMillis(long ms) {
    var s = CURRENT.get();
    if (s != null) {
      s.advanceFromTask(ms);
    }
  }

  private long nowMs = 0;
  private final PriorityQueue<ScheduledTask> pq =
      new PriorityQueue<>(Comparator.comparingLong(t -> t.nextRunAt));
  private final AtomicBoolean shutdown = new AtomicBoolean(false);

  enum Type {FIXED_RATE, FIXED_DELAY, ONE_SHOT}

  final class ScheduledTask implements ScheduledFuture<Object> {

    Runnable runnable;
    long nextRunAt;
    long periodMs; // for fixed-rate
    long delayMs;  // for fixed-delay
    Type type;
    volatile boolean cancelled;

    @Override
    public long getDelay(TimeUnit unit) {
      long d = Math.max(0, nextRunAt - nowMs);
      return unit.convert(d, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
      return Long.compare(this.getDelay(TimeUnit.MILLISECONDS),
          ((ScheduledTask) o).getDelay(TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      cancelled = true;
      return true;
    }

    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    @Override
    public boolean isDone() {
      return cancelled;
    } // good enough for tests

    @Override
    public Object get() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) {
      throw new UnsupportedOperationException();
    }
  }

  // ---- Deterministic time control for tests
  public long nowMs() {
    return nowMs;
  }

  public void advanceTimeBy(long deltaMs) {
    long target = nowMs + Math.max(0, deltaMs);
    while (!pq.isEmpty()) {
      ScheduledTask t = pq.peek();
      if (t.nextRunAt > target) {
        break;
      }

      pq.poll();                 // remove head
      if (t.cancelled) {
        continue; // <-- SKIP cancelled tasks, don't break
      }

      nowMs = t.nextRunAt;

      CURRENT.set(this);
      try {
        t.runnable.run();
      } finally {
        CURRENT.remove();
      }

      if (!t.cancelled) {
        switch (t.type) {
          case FIXED_RATE -> {
            t.nextRunAt += t.periodMs;
            pq.add(t);
          }
          case FIXED_DELAY -> {
            t.nextRunAt = nowMs + t.delayMs;
            pq.add(t);
          }
          case ONE_SHOT -> { /* no reschedule */ }
        }
      }
    }
    nowMs = target;
  }


  private void advanceFromTask(long ms) {
    nowMs += Math.max(0, ms);
  }

  // ---- ScheduledExecutorService minimal impls used by your clocks
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
      TimeUnit unit) {
    var t = new ScheduledTask();
    t.runnable = command;
    t.type = Type.FIXED_RATE;
    t.periodMs = unit.toMillis(period);
    t.nextRunAt = nowMs + unit.toMillis(initialDelay);
    pq.add(t);
    return t;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
      TimeUnit unit) {
    var t = new ScheduledTask();
    t.runnable = command;
    t.type = Type.FIXED_DELAY;
    t.delayMs = unit.toMillis(delay);
    t.nextRunAt = nowMs + unit.toMillis(initialDelay);
    pq.add(t);
    return t;
  }

  // The rest can be stubs — your clocks don't use them.
  @Override
  public void shutdown() {
    shutdown.set(true);
    pq.clear();
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown.set(true);
    pq.clear();
    return List.of();
  }

  @Override
  public boolean isShutdown() {
    return shutdown.get();
  }

  @Override
  public boolean isTerminated() {
    return shutdown.get();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) {
    return true;
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Future<?> submit(Runnable task) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
      TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void execute(Runnable command) {
    command.run();
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }
}
