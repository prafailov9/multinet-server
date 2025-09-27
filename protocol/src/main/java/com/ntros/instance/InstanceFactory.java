package com.ntros.instance;

import com.ntros.event.broadcaster.BroadcastToAll;
import com.ntros.event.broadcaster.BroadcastToOwnerOnly;
import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.listener.ClientSessionManager;
import com.ntros.instance.ins.Instance;
import com.ntros.instance.ins.WorldInstance;
import com.ntros.model.entity.config.access.Visibility;
import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.session.Session;
import com.ntros.ticker.WorldTicker;

public class InstanceFactory {

  public static Instance createInstance(Session session, boolean isShared,
      WorldConnector worldConnector) {
    Broadcaster broadcaster;
    if (isShared) {
      broadcaster = new BroadcastToAll();
    } else {
      broadcaster = new BroadcastToOwnerOnly(session.getSessionContext().getEntityId());
    }
    return new WorldInstance(worldConnector, new ClientSessionManager(),
        new WorldTicker(120), broadcaster, new InstanceConfig(100, false, Visibility.PUBLIC, true));
  }

}
