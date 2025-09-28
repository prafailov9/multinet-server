package com.ntros.lifecycle.instance;

import com.ntros.event.broadcaster.BroadcastToAll;
import com.ntros.event.broadcaster.BroadcastToOwnerOnly;
import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.sessionmanager.ClientSessionManager;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.state.solid.GridWorldState;
import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.clock.FixedRateClock;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InstanceFactory {

  private static final Map<String, Instance> INST = new ConcurrentHashMap<>();
  private static final Map<String, String> OWNER = new ConcurrentHashMap<>();

  public static Instance createInstance(Session session, boolean isShared,
      WorldConnector worldConnector) {
    Broadcaster broadcaster;
    if (isShared) {
      broadcaster = new BroadcastToAll();
    } else {
      broadcaster = new BroadcastToOwnerOnly(session.getSessionContext().getEntityId());
    }

    Instance inst = new ServerInstance(worldConnector, new ClientSessionManager(),
        new FixedRateClock(120), broadcaster,
        Settings.multiplayerDefault()
    );
    INST.put(worldConnector.getWorldName(), inst);
    return inst;
  }

  public static Instance createArena(String name, SessionManager sessions) {
    WorldConnector connector = new GridWorldConnector(new GridWorldState(name, 256, 256),
        new GridWorldEngine(), new WorldCapabilities(true, true, false, true));

    Settings cfg = Settings.multiplayerJoinable();
    Instance inst = new ServerInstance(connector, sessions, new FixedRateClock(30),
        new BroadcastToAll(), cfg);
    INST.put(name, inst);
    return inst;
  }

  public static Instance createSolo(String name, String ownerUserId,
      SessionManager sessions) {
    WorldConnector connector = new GridWorldConnector(new GridWorldState(name, 128, 128),
        new GridWorldEngine(), new WorldCapabilities(true, true, false, true));
    Settings cfg = Settings.singlePlayerDefault();
    Instance inst = new ServerInstance(connector, sessions, new FixedRateClock(30),
        new BroadcastToOwnerOnly(ownerUserId), cfg);
    INST.put(name, inst);
    OWNER.put(name, ownerUserId);
    return inst;
  }

  public static Instance createLife(String name, String ownerUserId,
      SessionManager sessions) {
    // If you have a LifeEngine/State, plug it here; otherwise reuse grid placeholder
    WorldConnector connector = new GridWorldConnector(new GridWorldState(name, 256, 256),
        new GridWorldEngine(), new WorldCapabilities(true, true, false, true));
    Settings cfg = Settings.singlePlayerOrchestrator();
    Instance inst = new ServerInstance(connector, sessions, new FixedRateClock(20),
        new BroadcastToOwnerOnly(ownerUserId), cfg);
    INST.put(name, inst);
    OWNER.put(name, ownerUserId);
    return inst;
  }

  public static Instance get(String name) {
    return INST.get(name);
  }

  public static Collection<Instance> all() {
    return INST.values();
  }

  public static Optional<String> ownerOf(String name) {
    return Optional.ofNullable(OWNER.get(name));
  }
}
