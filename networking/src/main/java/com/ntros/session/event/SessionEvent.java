package com.ntros.session.event;

import com.ntros.session.Session;

public class SessionEvent {

    private EventType eventType;

    private Session session;

    private String reason;

    public SessionEvent(EventType eventType, Session session, String reason) {
        this.eventType = eventType;
        this.session = session;
        this.reason = reason;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Session getSession() {
        return session;
    }

    public String getReason() {
        return reason;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
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
                "eventType=" + eventType +
                ", session=" + session +
                ", reason='" + reason + '\'' +
                '}';
    }
}
