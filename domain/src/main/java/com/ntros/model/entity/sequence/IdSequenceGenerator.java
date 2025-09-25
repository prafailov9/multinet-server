package com.ntros.model.entity.sequence;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class IdSequenceGenerator {

  private static final long START_AT = 1L;

  // Make them instance fields to match the singleton model (cleaner)
  private final AtomicLong sessionSeq = new AtomicLong(START_AT);
  private final AtomicLong worldSeq = new AtomicLong(START_AT);
  private final AtomicLong playerSeq = new AtomicLong(START_AT);
  private final AtomicLong npcSeq = new AtomicLong(START_AT);

  private IdSequenceGenerator() {
  }

  private static class Holder {

    static final IdSequenceGenerator INSTANCE = new IdSequenceGenerator();
  }

  public static IdSequenceGenerator getInstance() {
    return Holder.INSTANCE;
  }

  // --- Session/world/entity IDs ---
  public long nextSessionId() {
    return sessionSeq.getAndIncrement();
  }

  public long nextWorldId() {
    return worldSeq.getAndIncrement();
  }

  public long nextPlayerEntityId() {
    return playerSeq.getAndIncrement();
  }

  public long nextNpcEntityId() {
    return npcSeq.getAndIncrement();
  }

  // Optional: typed resets
  public void resetAll() {
    sessionSeq.set(START_AT);
    worldSeq.set(START_AT);
    playerSeq.set(START_AT);
    npcSeq.set(START_AT);
  }

  // If you still want RNG, don’t cache it:
  public static int nextInt(int originInclusive, int boundExclusive) {
    return java.util.concurrent.ThreadLocalRandom.current()
        .nextInt(originInclusive, boundExclusive);
  }
}
