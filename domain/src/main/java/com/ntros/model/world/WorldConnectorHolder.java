package com.ntros.model.world;

import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.state.solid.GridWorldState;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class WorldConnectorHolder {

  private static final WorldConnector DEFAULT_WORLD = new GridWorldConnector(
      new GridWorldState("world-1", 10, 10), new GridWorldEngine(),
      new WorldCapabilities(true, true, false, true));
  private static final Map<String, WorldConnector> WORLDS = new HashMap<>();

  static {
    // default available world
    WORLDS.put("world-1", DEFAULT_WORLD);
    WORLDS.put("world-2",
        new GridWorldConnector(new GridWorldState("world-2", 5, 5), new GridWorldEngine(),
            new WorldCapabilities(true, true, false, true)));
    WORLDS.put("world-3",
        new GridWorldConnector(new GridWorldState("world-3", 7, 7), new GridWorldEngine(),
            new WorldCapabilities(true, true, false, true)));
    WORLDS.put("arena-x",
        new GridWorldConnector(new GridWorldState("arena-x", 3, 3), new GridWorldEngine(),
            new WorldCapabilities(true, true, false, true)));
    WORLDS.put("arena-y",
        new GridWorldConnector(new GridWorldState("arena-y", 9, 9), new GridWorldEngine(),
            new WorldCapabilities(true, true, false, true)));

    WORLDS.put("gol-x",
        new GridWorldConnector(new GridWorldState("gol-x", 9, 9), new GridWorldEngine(),
            new WorldCapabilities(true, true, false, true)));
    WORLDS.put("gol-y",
        new GridWorldConnector(new GridWorldState("gol-y", 9, 9), new GridWorldEngine(),
            new WorldCapabilities(true, true, false, true)));
    WORLDS.put("gol-z",
        new GridWorldConnector(new GridWorldState("gol-z", 9, 9), new GridWorldEngine(),
            new WorldCapabilities(true, true, false, true)));

  }

  private WorldConnectorHolder() {
  }

  public static void register(WorldConnector worldConnector) {
    WORLDS.put(worldConnector.worldName(), worldConnector);
  }

  public static void remove(String id) {
    WORLDS.remove(id);
  }

  public static WorldConnector getWorld(String id) {
    return WORLDS.get(id);
  }

  public static WorldConnector getDefaultWorld() {
    return DEFAULT_WORLD;
  }

  public static Collection<WorldConnector> getAllWorlds() {
    return WORLDS.values();
  }

  public static void clear() {
    WORLDS.clear();
  }

}
