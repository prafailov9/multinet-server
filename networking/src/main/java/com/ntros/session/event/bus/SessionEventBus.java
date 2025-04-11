package com.ntros.session.event.bus;

import com.ntros.session.event.SessionEvent;

import java.util.ArrayList;
import java.util.List;

public class SessionEventBus implements EventBus {

    List<SessionEventListener> listeners = new ArrayList<>();

    @Override
    public void register(SessionEventListener sessionEventListener) {
        listeners.add(sessionEventListener);
    }

    @Override
    public void remove(SessionEventListener sessionEventListener) {
        listeners.remove(sessionEventListener);
    }

    @Override
    public void publish(SessionEvent sessionEvent) {
        for (SessionEventListener listener: listeners) {
            listener.onSessionEvent(sessionEvent);
        }
    }
}
