package com.ntros.session.event;

import com.ntros.message.ProtocolContext;
import com.ntros.model.world.WorldRegistry;

public class SessionCleaner implements SessionEventListener {

    @Override
    public void onSessionEvent(SessionEvent sessionEvent) {
        ProtocolContext context = null;
        if (!sessionEvent.getEventType().equals(EventType.SESSION_STARTED)) {
            context = sessionEvent.getSession().getProtocolContext();
        }

        if (context == null) {
            return;
        }

        if (context.getSessionId() != null && !context.getSessionId().isEmpty() && context.getWorldId() != null && !context.getWorldId().isEmpty()) {
            WorldRegistry.getGridWorld(context.getWorldId()).remove(context.getSessionId());
        }

    }
}
