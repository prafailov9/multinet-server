package com.ntros.model.entity.sequence;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class IdSequenceGenerator {

    public static final ThreadLocalRandom RNG = ThreadLocalRandom.current();
    private static final int DEFAULT_INITIAL_ID = 0;

    private static final AtomicLong WORLD_COUNTER = new AtomicLong(DEFAULT_INITIAL_ID);
    private static final AtomicLong PLAYER_ENTITY_COUNTER = new AtomicLong(DEFAULT_INITIAL_ID);
    private static final AtomicLong NPC_ENTITY_COUNTER = new AtomicLong(DEFAULT_INITIAL_ID);

    private IdSequenceGenerator() {

    }

    static class InstanceHolder {

        static IdSequenceGenerator INSTANCE = new IdSequenceGenerator();

    }

    public static IdSequenceGenerator getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public long getNextNpcId() {
        return NPC_ENTITY_COUNTER.incrementAndGet();
    }

    public long getNextPlayerId() {
        return PLAYER_ENTITY_COUNTER.incrementAndGet();
    }

    public long getNextWorldId() {
        return WORLD_COUNTER.incrementAndGet();
    }


}
