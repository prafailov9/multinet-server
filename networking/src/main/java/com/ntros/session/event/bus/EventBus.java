package com.ntros.session.event.bus;

import com.ntros.session.event.SessionEvent;

public interface EventBus {

    void register(SessionEventListener sessionEventListener);
    void remove(SessionEventListener sessionEventListener);
    void publish(SessionEvent sessionEvent);

}
