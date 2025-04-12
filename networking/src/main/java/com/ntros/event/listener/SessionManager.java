package com.ntros.event.listener;

import com.ntros.session.Session;

public interface SessionManager extends SessionEventListener {

    void broadcast(String serverMessage);

    void register(Session session);

    void remove(Session session);

    int activeSessions();

    void shutdownAll();

}
