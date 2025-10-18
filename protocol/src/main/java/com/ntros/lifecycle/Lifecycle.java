package com.ntros.lifecycle;

public interface Lifecycle {

  void start();             // Start (idempotent). After shutdown(), starting may throw.

  void stop();                 // Graceful stop (idempotent). Resources remain reusable.

  boolean isRunning();         // True if actively running, not paused/stopped/shutdown.

}
