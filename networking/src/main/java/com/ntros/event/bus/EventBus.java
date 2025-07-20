package com.ntros.event.bus;

import com.ntros.event.SessionEvent;
import com.ntros.event.listener.SessionEventListener;

import java.util.Collection;

public interface EventBus {

    void register(SessionEventListener sessionEventListener);
    void registerAll(Collection<SessionEventListener> sessionEventListeners);

    void remove(SessionEventListener sessionEventListener);
    void removeAll();
    void publish(SessionEvent sessionEvent);

}
