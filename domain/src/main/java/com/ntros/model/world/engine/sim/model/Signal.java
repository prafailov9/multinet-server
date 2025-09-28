package com.ntros.model.world.engine.sim.model;

import java.util.HashMap;
import java.util.Map;

// Simple traffic light (extend with phases/groups as needed)
public final class Signal {

  private final NodeId node;
  private final Map<LinkId, Boolean> linkGreen = new HashMap<>(); // true = go

  public Signal(NodeId node) {
    this.node = node;
  }

  public NodeId node() {
    return node;
  }

  public boolean isGreen(LinkId incoming) {
    return linkGreen.getOrDefault(incoming, true);
  }

  // engine-only
  void setGreen(LinkId incoming, boolean green) {
    linkGreen.put(incoming, green);
  }
}