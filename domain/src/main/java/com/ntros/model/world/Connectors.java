package com.ntros.model.world;

import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.d2.grid.GridWorldEngine;
import com.ntros.model.world.state.grid.ArenaGridState;
import java.util.HashMap;
import java.util.Map;

public final class Connectors {

  private static final Map<String, WorldConnector> WORLDS = new HashMap<>();


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
