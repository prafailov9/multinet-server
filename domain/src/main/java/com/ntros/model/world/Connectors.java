package com.ntros.model.world;

import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.d2.grid.GridWorldEngine;
import com.ntros.model.world.state.d2.grid.GridWorldState;
import java.util.HashMap;
import java.util.Map;

public final class Connectors {

  private static final WorldConnector DEFAULT_WORLD = new GridWorldConnector(
      new GridWorldState("world-1", 10, 10), new GridWorldEngine(),
      new WorldCapabilities(true, true, false, true));
  private static final Map<String, WorldConnector> WORLDS = new HashMap<>();

  static {
    WORLDS.put("world-1", DEFAULT_WORLD);
  }

  private Connectors() {
  }

  public static void register(WorldConnector worldConnector) {
    WORLDS.put(worldConnector.getWorldName(), worldConnector);
  }

  public static void remove(String worldName) {
    WORLDS.remove(worldName);
  }

  public static void clear() {
    WORLDS.clear();
  }

}
