package com.ntros.session.event;

import com.ntros.session.event.bus.SessionEventListener;

public interface EventBus {

    void register(SessionEventListener sessionEventListener);
    void remove(SessionEventListener sessionEventListener);
    void publish(SessionEvent sessionEvent);

}
