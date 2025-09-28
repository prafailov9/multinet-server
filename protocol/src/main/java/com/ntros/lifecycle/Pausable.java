package com.ntros.lifecycle;

/** Pause/resume without tearing down resources. */
public interface Pausable {
  void pause();
  void resume();
  boolean isPaused();
}