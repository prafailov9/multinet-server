package com.ntros.instance.ins;

import com.ntros.event.listener.SessionManager;
import com.ntros.instance.runner.InstanceRunner;
import com.ntros.model.world.connector.WorldConnector;

public abstract class AbstractInstance implements Instance {

  protected final WorldConnector worldConnector;
  protected final InstanceRunner instanceRunner;
  protected final SessionManager sessionManager;

  AbstractInstance(WorldConnector worldConnector, InstanceRunner instanceRunner,
      SessionManager sessionManager) {
    this.worldConnector = worldConnector;
    this.instanceRunner = instanceRunner;
    this.sessionManager = sessionManager;
  }


}
