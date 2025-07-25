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

  private SessionEvent(SessionEventType sessionEventType, String reason) {
    this.sessionEventType = sessionEventType;
    this.reason = reason;
  }

  private SessionEvent(SessionEventType sessionEventType, Session session, String reason) {
    this.sessionEventType = sessionEventType;
    this.session = session;
    this.reason = reason;
  }

  private SessionEvent(SessionEventType sessionEventType, Long sessionId, String reason) {
    this.sessionEventType = sessionEventType;
    this.sessionId = sessionId;
    this.reason = reason;
  }

  private SessionEvent(SessionEventType sessionEventType, Session session, String reason,
      String serverMessage) {
    this.sessionEventType = sessionEventType;
    this.session = session;
    this.reason = reason;
    this.serverMessage = serverMessage;
  }

  private SessionEvent(SessionEventType sessionEventType, Long sessionId, String reason,
      String serverMessage) {
    this.sessionEventType = sessionEventType;
    this.sessionId = sessionId;
    this.reason = reason;
    this.serverMessage = serverMessage;
  }

  public static SessionEvent sessionStarted(Session session, String reason,
      String serverWelcomeMessage) {
    return new SessionEvent(SessionEventType.SESSION_STARTED, session, reason,
        serverWelcomeMessage);
  }

  public static SessionEvent sessionStarted(Long sessionId, String reason,
      String serverWelcomeMessage) {
    return new SessionEvent(SessionEventType.SESSION_STARTED, sessionId, reason,
        serverWelcomeMessage);
  }


  public static SessionEvent sessionClosed(Session session, String reason) {
    return new SessionEvent(SessionEventType.SESSION_CLOSED, session, reason);
  }

  public static SessionEvent sessionClosed(Long sessionId, String reason) {
    return new SessionEvent(SessionEventType.SESSION_CLOSED, sessionId, reason);
  }

  public static SessionEvent sessionFailed(Session session, String reason) {
    return new SessionEvent(SessionEventType.SESSION_FAILED, session, reason);
  }

  public static SessionEvent sessionFailed(Long sessionId, String reason) {
    return new SessionEvent(SessionEventType.SESSION_FAILED, sessionId, reason);
  }

  public static SessionEvent sessionRestarted(Long sessionId, String reason) {
    return new SessionEvent(SessionEventType.SESSION_RESTARTED, sessionId, reason);
  }

  public static SessionEvent sessionShutdownAll(String reason) {
    return new SessionEvent(SessionEventType.SESSION_SHUTDOWN_ALL, reason);
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
