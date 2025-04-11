package com.ntros.event.bus;

import com.ntros.event.SessionEvent;

public interface SessionEventListener {

    void onSessionEvent(SessionEvent sessionEvent);

}
