package com.ntros.event.listener;

import com.ntros.session.Session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerSessionManager implements SessionManager {
    private static final Logger LOGGER = Logger.getLogger(ServerSessionManager.class.getName());
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private final Map<Long, Session> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void register(Session session) {
        sessions.add(session);
        sessionMap.put(session.getProtocolContext().getSessionId(), session);
        LOGGER.log(Level.INFO, "Registered session: {0}", session.getProtocolContext().getSessionId());
    }

    @Override
    public void remove(Session session) {
        sessions.remove(session);
        LOGGER.log(Level.INFO, "Removed session: {0}", session.getProtocolContext().getSessionId());
    }

    @Override
    public int activeSessions() {
        return sessions.size();
    }


    @Override
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

    @Override
    public void broadcast(String serverMessage) {
        for (Session session : sessions) {
            session.accept(serverMessage);
        }
    }
}
