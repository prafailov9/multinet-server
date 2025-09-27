package com.ntros.instance;

import com.ntros.event.broadcaster.BroadcastToAll;
import com.ntros.event.broadcaster.BroadcastToOwnerOnly;
import com.ntros.event.listener.SessionManager;
import com.ntros.instance.ins.WorldInstance;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.entity.config.access.Visibility;
import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.state.solid.GridWorldState;
import com.ntros.ticker.WorldTicker;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldRegistry {

  private static final Map<String, WorldInstance> INST = new ConcurrentHashMap<>();
  private static final Map<String, String> OWNER = new ConcurrentHashMap<>();

  public static WorldInstance createArena(String name,
      SessionManager sessions) {
    WorldConnector connector = new GridWorldConnector(
        new GridWorldState(name, 256, 256),
        new GridWorldEngine(),
        new WorldCapabilities(true, true, false, true)
    );
    InstanceConfig cfg = new InstanceConfig(16, false, Visibility.JOINABLE, true);
    WorldInstance inst = new WorldInstance(connector, sessions,
        new WorldTicker(30), new BroadcastToAll(), cfg);
    INST.put(name, inst);
    return inst;
  }

  public static WorldInstance createSolo(String name, String ownerUserId,
      SessionManager sessions) {
    WorldConnector connector = new GridWorldConnector(
        new GridWorldState(name, 128, 128),
        new GridWorldEngine(),
        new WorldCapabilities(true, true, false, true)
    );
    InstanceConfig cfg = new InstanceConfig(1, false, Visibility.PRIVATE, false);
    WorldInstance inst = new WorldInstance(connector, sessions,
        new WorldTicker(30), new BroadcastToOwnerOnly(ownerUserId), cfg);
    INST.put(name, inst);
    OWNER.put(name, ownerUserId);
    return inst;
  }

  public static WorldInstance createLife(String name, String ownerUserId,
      SessionManager sessions) {
    // If you have a LifeEngine/State, plug it here; otherwise reuse grid placeholder
    WorldConnector connector = new GridWorldConnector(
        new GridWorldState(name, 256, 256),
        new GridWorldEngine(),
        new WorldCapabilities(true, true, false, true)
    );
    InstanceConfig cfg = new InstanceConfig(1, true, Visibility.PRIVATE, false);
    WorldInstance inst = new WorldInstance(connector, sessions,
        new WorldTicker(20), new BroadcastToOwnerOnly(ownerUserId), cfg);
    INST.put(name, inst);
    OWNER.put(name, ownerUserId);
    return inst;
  }

  public static WorldInstance get(String name) {
    return INST.get(name);
  }

  public static Collection<WorldInstance> all() {
    return INST.values();
  }

  public static Optional<String> ownerOf(String name) {
    return Optional.ofNullable(OWNER.get(name));
  }
}
