package com.ntros.event.broadcaster;

import com.ntros.event.listener.SessionManager;

public interface Broadcaster {

  void publish(String serializedState, SessionManager sessions);
}
