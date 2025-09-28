package com.ntros.lifecycle;

/**
 * Reset internal state to initial (often implies stop first).
 */
public interface Resettable {

  void reset();
}