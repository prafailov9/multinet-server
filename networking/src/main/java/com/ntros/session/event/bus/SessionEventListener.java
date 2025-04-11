package com.ntros.session.event.bus;

import com.ntros.session.event.SessionEvent;

public interface SessionEventListener {

    void onSessionEvent(SessionEvent sessionEvent);

}
