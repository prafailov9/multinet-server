package com.ntros.event.bus;

import com.ntros.event.SessionEvent;
import com.ntros.event.listener.SessionEventListener;

public interface EventBus {

    void register(SessionEventListener sessionEventListener);

    void remove(SessionEventListener sessionEventListener);

    void publish(SessionEvent sessionEvent);

}
