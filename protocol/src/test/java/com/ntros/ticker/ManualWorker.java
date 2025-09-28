package com.ntros.ticker;

import java.util.concurrent.AbstractExecutorService;

// ManualWorker: deterministic single-thread worker you drive in tests
final class ManualWorker extends AbstractExecutorService {

  private final java.util.concurrent.BlockingQueue<Runnable> q = new java.util.concurrent.LinkedBlockingQueue<>();
  private volatile boolean shutdown;

  @Override
  public void execute(Runnable command) {
    if (!shutdown) {
      q.add(command);
    }
  }

  public int pending() {
    return q.size();
  }

  public void runNext() {
    var r = q.poll();
    if (r != null) {
      r.run();
    }
  }

  public int drainAll() {
    int n = 0;
    Runnable r;
    while ((r = q.poll()) != null) {
      r.run();
      n++;
    }
    return n;
  }

  @Override
  public void shutdown() {
    shutdown = true;
    q.clear();
  }

  @Override
  public java.util.List<Runnable> shutdownNow() {
    shutdown();
    return java.util.List.of();
  }

  @Override
  public boolean isShutdown() {
    return shutdown;
  }

  @Override
  public boolean isTerminated() {
    return shutdown && q.isEmpty();
  }

  @Override
  public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) {
    return true;
  }
}
