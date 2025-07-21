package com.ntros.event.listener;

import com.ntros.session.Session;

public interface SessionManager {

    // Broadcasts to all registered sessions
    void broadcast(String serverMessage);

    void register(Session session);

    void remove(Session session);

    int activeSessions();

    void shutdownAll();

}
