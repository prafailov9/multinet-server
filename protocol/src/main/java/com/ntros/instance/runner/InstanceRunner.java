package com.ntros.instance.runner;

import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.model.entity.config.access.WorldConfig;
import com.ntros.ticker.Ticker;

public final class InstanceRunner {

  private final WorldConfig config;
  private final Ticker ticker;
  private final Broadcaster broadcaster;   // Strategy

  private InstanceRunner(WorldConfig config, Ticker ticker, Broadcaster broadcaster) {
    this.config = config;
    this.ticker = ticker;
    this.broadcaster = broadcaster;
  }

  public WorldConfig getConfig() {
    return config;
  }

  public Ticker getTicker() {
    return ticker;
  }

  public Broadcaster getBroadcaster() {
    return broadcaster;
  }
}
