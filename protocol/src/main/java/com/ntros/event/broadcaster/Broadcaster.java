package com.ntros.event.broadcaster;

import com.ntros.event.sessionmanager.SessionManager;

public interface Broadcaster {

  void publish(String serializedState, SessionManager sessions);

}
