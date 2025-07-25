package com.ntros.instance;

import com.ntros.session.Session;

public interface Instance {

  void run();

  String worldName();

  void reset();

  void registerSession(Session session);

  void removeSession(Session session);

  int getActiveSessionsCount();

  boolean isRunning();

}
