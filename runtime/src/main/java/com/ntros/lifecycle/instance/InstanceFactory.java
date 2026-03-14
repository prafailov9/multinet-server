package com.ntros.lifecycle.instance;

import com.ntros.broadcast.SharedBroadcaster;
import com.ntros.broadcast.SingleBroadcaster;
import com.ntros.broadcast.Broadcaster;
import com.ntros.lifecycle.sessionmanager.ClientSessionManager;
import com.ntros.lifecycle.sessionmanager.SessionManager;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.entity.config.access.InstanceSettings;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.d2.grid.gameoflife.GameOfLifeEngine;
import com.ntros.model.world.engine.d2.grid.GridWorldEngine;
import com.ntros.model.world.state.d2.grid.GridWorldState;
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
    Broadcaster broadcaster = isShared ? new SharedBroadcaster()
        : new SingleBroadcaster(session.getSessionContext().getEntityId());

    Instance inst = new ServerInstance(worldConnector, new ClientSessionManager(),
        new FixedRateClock(120), broadcaster,
        InstanceSettings.multiplayerDefault()
    );
    INST.put(worldConnector.getWorldName(), inst);
    return inst;
  }

  public static Instance createArena(String name, SessionManager sessions) {
    WorldConnector connector = new GridWorldConnector(new GridWorldState(name, 256, 256),
        new GridWorldEngine(), new WorldCapabilities(true, true, false, true));

    InstanceSettings cfg = InstanceSettings.multiplayerJoinable();
    Instance inst = new ServerInstance(connector, sessions, new FixedRateClock(30),
        new SharedBroadcaster(), cfg);
    INST.put(name, inst);
    return inst;
  }

  public static Instance createSolo(String name, String ownerUserId,
      SessionManager sessions) {
    WorldConnector connector = new GridWorldConnector(new GridWorldState(name, 128, 128),
        new GridWorldEngine(), new WorldCapabilities(true, true, false, true));
    InstanceSettings cfg = InstanceSettings.singlePlayerDefault();
    Instance inst = new ServerInstance(connector, sessions, new FixedRateClock(30),
        new SingleBroadcaster(ownerUserId), cfg);
    INST.put(name, inst);
    OWNER.put(name, ownerUserId);
    return inst;
  }

  public static Instance createGameOfLifeWorld(String name, String ownerUserId,
      SessionManager sessions) {
    WorldConnector connector = new GridWorldConnector(
        GridWorldState.blank(name, 256, 256),
        new GameOfLifeEngine(),
        new WorldCapabilities(true, true, true, true));
    InstanceSettings cfg = InstanceSettings.singlePlayerOrchestrator();
    Instance inst = new ServerInstance(connector, sessions, new FixedRateClock(20),
        new SingleBroadcaster(ownerUserId), cfg);
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
