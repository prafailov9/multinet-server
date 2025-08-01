package com.ntros.instance;

import com.ntros.session.Session;
import java.util.Map;

public interface Instance {

  void run();

  String worldName();

  void reset();
  void registerSession(Session session);

  void removeSession(Session session);

  int getActiveSessionsCount();

  boolean isRunning();

  void pause();
  void resume();
  void updateTickRate(int ticksPerSecond);

  // TODO: Refactor worldStateUpdates Map to store Generic world types.
  void updateWorldState(Map<Boolean, Boolean> worldStateUpdates);

}
