package com.ntros.event.bus;

import com.ntros.event.SessionEvent;
import com.ntros.event.listener.SessionEventListener;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.extern.slf4j.Slf4j;

/**
 * Broadcasts session events to all attached listeners. Singleton.
 */
@Slf4j
public class SessionEventBus implements EventBus {

  private final Set<SessionEventListener> listeners = new CopyOnWriteArraySet<>();

  private SessionEventBus() {
  }

  public static SessionEventBus get() {
    return InstanceHolder.INSTANCE;
  }

  @Override
  public void register(SessionEventListener sessionEventListener) {
    log.info("[IN SESSION_EVENT_BUS]: Registering SessionEventListener: {}", sessionEventListener);

    listeners.add(sessionEventListener);
  }

  @Override
  public void registerAll(Collection<SessionEventListener> sessionEventListeners) {
    listeners.addAll(sessionEventListeners);
  }

  @Override
  public void remove(SessionEventListener sessionEventListener) {
    log.info("Removing SessionEventListener: {}", sessionEventListener);
    listeners.remove(sessionEventListener);
  }

  @Override
  public void removeAll() {
    log.info("Removing SessionEventListeners: {}", listeners);
    listeners.clear();
  }

  @Override
  public void publish(SessionEvent sessionEvent) {
    log.info("Publishing {} for session: {}", sessionEvent.getEventType(),
        sessionEvent.getSession().getProtocolContext());
    for (SessionEventListener listener : listeners) {
      log.info("Notifying listener {} with event {}", listener, sessionEvent);
      listener.onSessionEvent(sessionEvent);
    }
  }

  private static class InstanceHolder {

    static final SessionEventBus INSTANCE = new SessionEventBus();
  }
}
