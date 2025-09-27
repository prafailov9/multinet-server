package com.ntros.instance.ins;

import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.model.world.connector.WorldConnector;

public abstract class AbstractInstance implements Instance {

  protected final WorldConnector worldConnector;
  protected final SessionManager sessionManager;

  AbstractInstance(WorldConnector worldConnector,
      SessionManager sessionManager) {
    this.worldConnector = worldConnector;
    this.sessionManager = sessionManager;
  }


}
