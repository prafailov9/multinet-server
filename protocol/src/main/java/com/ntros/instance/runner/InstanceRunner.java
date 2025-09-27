package com.ntros.instance.runner;

import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.ticker.Ticker;

public final class InstanceRunner {

  private final InstanceConfig config;
  private final Ticker ticker;
  private final Broadcaster broadcaster;   // Strategy

  private InstanceRunner(InstanceConfig config, Ticker ticker, Broadcaster broadcaster) {
    this.config = config;
    this.ticker = ticker;
    this.broadcaster = broadcaster;
  }

  public InstanceConfig getConfig() {
    return config;
  }

  public Ticker getTicker() {
    return ticker;
  }

  public Broadcaster getBroadcaster() {
    return broadcaster;
  }
}
