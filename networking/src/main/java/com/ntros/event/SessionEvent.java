package com.ntros.event;

import com.ntros.session.Session;

public class SessionEvent {

    private SessionEventType sessionEventType;
    private Long sessionId;
    private Session session;

    private String reason;

    public SessionEvent(SessionEventType sessionEventType, Session session, String reason) {
        this.sessionEventType = sessionEventType;
        this.session = session;
        this.reason = reason;
    }

    public SessionEvent(SessionEventType sessionEventType, Long sessionId ,String reason) {
        this.sessionEventType = sessionEventType;
        this.sessionId = sessionId;
        this.reason = reason;
    }

    public SessionEventType getEventType() {
        return sessionEventType;
    }

    public Session getSession() {
        return session;
    }

    public String getReason() {
        return reason;
    }

    public void setEventType(SessionEventType sessionEventType) {
        this.sessionEventType = sessionEventType;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "SessionEvent{" +
                "eventType=" + sessionEventType +
                ", session=" + session +
                ", reason='" + reason + '\'' +
                '}';
    }
}
