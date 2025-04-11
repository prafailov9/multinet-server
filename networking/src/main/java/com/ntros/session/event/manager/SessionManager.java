package com.ntros.session.event;

import com.ntros.session.Session;

public interface SessionManager {


    void register(Session session);

    void remove(Session session);

    void shutdownAll();

    void broadcast(String serverMessage);
}
