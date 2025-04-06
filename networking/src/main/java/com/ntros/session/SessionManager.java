package com.ntros.session;

import com.ntros.session.event.SessionEvent;
import com.ntros.session.event.SessionEventListener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionManager implements SessionEventListener {

    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());


    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    public void register(Session session) {
        sessions.add(session);
        LOGGER.log(Level.INFO, "Registered session: {0}", session.getProtocolContext().getSessionId());
    }

    public void remove(Session session) {
        sessions.remove(session);
        LOGGER.log(Level.INFO, "Removed session: {0}", session.getProtocolContext().getSessionId());
    }


    public void shutdownAll() {
        for (Session session : sessions) {
            try {
                session.terminate();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to close session: {0}", session.getProtocolContext().getSessionId());
            }
        }
        sessions.clear();
        LOGGER.info("All sessions shut down.");
    }

    public void broadcast(String serverMessage) {
        for (Session session : sessions) {
            session.send(serverMessage);
        }
    }

    @Override
    public void onSessionEvent(SessionEvent sessionEvent) {
        switch (sessionEvent.getEventType()) {
            case SESSION_STARTED -> register(sessionEvent.getSession());
            case SESSION_CLOSED -> remove(sessionEvent.getSession());
        }
//        sessions.stream().forEach(session -> session.);
    }
}
