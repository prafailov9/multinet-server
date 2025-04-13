package com.ntros.event;

import com.ntros.session.Session;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionEvent {

    private SessionEventType sessionEventType;
    private Long sessionId;
    private Session session;
    private String reason;
    private String serverMessage;

    private SessionEvent(SessionEventType sessionEventType, Session session, String reason) {
        this.sessionEventType = sessionEventType;
        this.session = session;
        this.reason = reason;
    }

    private SessionEvent(SessionEventType sessionEventType, Session session, String reason, String serverMessage) {
        this.sessionEventType = sessionEventType;
        this.session = session;
        this.reason = reason;
        this.serverMessage = serverMessage;
    }

    public static SessionEvent ofSessionStarted(Session session, String reason, String serverWelcomeMessage) {
        return new SessionEvent(SessionEventType.SESSION_STARTED, session, reason, serverWelcomeMessage);
    }

    public static SessionEvent ofSessionClosed(Session session, String reason) {
        return new SessionEvent(SessionEventType.SESSION_CLOSED, session, reason);
    }

    public static SessionEvent ofSessionFailed(Session session, String reason) {
        return new SessionEvent(SessionEventType.SESSION_FAILED, session, reason);
    }

    public SessionEventType getEventType() {
        return sessionEventType;
    }

    public void setEventType(SessionEventType sessionEventType) {
        this.sessionEventType = sessionEventType;
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
