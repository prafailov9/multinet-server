package com.ntros.lifecycle;

import java.util.concurrent.CompletableFuture;

/**
 * Ability to await internal queues to empty (or a control thread to reach a barrier).
 */
public interface Drainable {

  CompletableFuture<Void> drain();
}