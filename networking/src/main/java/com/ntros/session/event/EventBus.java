package com.ntros.session.event;

public interface EventBus {

    void register(SessionEventListener sessionEventListener);
    void remove(SessionEventListener sessionEventListener);
    void publish(SessionEvent sessionEvent);

}
