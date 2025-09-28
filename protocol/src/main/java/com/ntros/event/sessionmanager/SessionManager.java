package com.ntros.event.sessionmanager;

import com.ntros.lifecycle.session.Session;
import java.util.List;

public interface SessionManager {

  // Broadcasts to all registered sessions
  void broadcast(String serverMessage);

  void register(Session session);

  void remove(Session session);

  int activeSessionsCount();

  List<Session> getActiveSessions();

  Session getSession(String entityId);

  void shutdownAll();

}
