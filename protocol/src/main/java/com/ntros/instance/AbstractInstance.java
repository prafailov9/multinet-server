package com.ntros.instance;

import com.ntros.model.world.connector.WorldConnector;
import com.ntros.ticker.Ticker;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractInstance implements Instance {


    private final WorldConnector worldConnector;
    private final Ticker ticker;
    private final AtomicBoolean tickerRunning = new AtomicBoolean(false);

    AbstractInstance(WorldConnector worldConnector, Ticker ticker) {
        this.worldConnector = worldConnector;
        this.ticker = ticker;
    }


}
