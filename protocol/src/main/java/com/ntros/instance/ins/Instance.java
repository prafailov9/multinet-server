package com.ntros.instance.ins;

import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.session.Session;
import java.util.Map;

public interface Instance {

  void run();

  InstanceConfig getConfig();

  WorldConnector getWorldConnector();

  String getWorldName();

  void reset();

  void registerSession(Session session);

  void removeSession(Session session);

  Session getSession(Long sessionId);

  int getActiveSessionsCount();

  boolean isRunning();

  void pause();

  void resume();

  void updateTickRate(int ticksPerSecond);

  // TODO: Refactor worldStateUpdates Map to store Generic world types.
  void updateWorldState(Map<Boolean, Boolean> worldStateUpdates);

}
