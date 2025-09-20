package com.ntros.event.listener;

import com.ntros.session.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientSessionManager implements SessionManager {

    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private final Map<Long, Session> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void broadcast(String serverMessage) {
        for (Session session : sessions) {
            session.response(serverMessage);
        }
    }

    @Override
    public void register(Session session) {
        sessions.add(session);
        sessionMap.put(session.getProtocolContext().getSessionId(), session);
        log.info("Registered session: {}", session.getProtocolContext().getSessionId());
    }

    @Override
    public void remove(Session session) {
        sessions.remove(session);
        sessionMap.remove(session.getProtocolContext().getSessionId());
        log.info("Removed session: {}", session.getProtocolContext().getSessionId());
    }

    @Override
    public int activeSessionsCount() {
        return sessions.size();
    }

    @Override
    public List<Session> getActiveSessions() {
        return new ArrayList<>(sessionMap.values());
    }


    @Override
    public void shutdownAll() {
        log.info("SessionManger: current active sessions count: {}", sessions.size());
        if (sessions.isEmpty()) {
            log.info("SessionManager: no active sessions, nothing to shut down.");
            return;
        }
        for (Session session : sessions) {
            try {
                log.info("SessionManager: Sending stop signal for session {}",
                        session.getProtocolContext());
                session.stop();
            } catch (Exception ex) {
                log.error("Failed to close session: {}", session.getProtocolContext().getSessionId());
            }
        }
        sessions.clear();
        log.info("All sessions shut down.");
    }
}
