package com.ntros.instance;

import com.ntros.event.listener.ClientSessionManager;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.state.solid.GridWorldState;
import com.ntros.session.Session;
import com.ntros.ticker.WorldTicker;

// TODO: need to dynamically create new instances
public class InstanceFactory {

  public static Instance createWorld(Session session, boolean isShared,
      WorldConnector worldConnector) {
    return isShared
        ? new MultiSessionInstance(worldConnector, new ClientSessionManager(), new WorldTicker(120))
        : new SingleSessionInstance(worldConnector, new WorldTicker(120), session);
  }

  public static SingleSessionInstance createWorld(Session session) {
    return new SingleSessionInstance(
        new GridWorldConnector(new GridWorldState("arena-x", 3, 3), new GridWorldEngine()),
        new WorldTicker(120), session);
  }

}
