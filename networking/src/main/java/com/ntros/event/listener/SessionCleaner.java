package com.ntros.event.listener;

import com.ntros.message.ProtocolContext;
import com.ntros.model.world.WorldDispatcher;
import com.ntros.model.world.context.WorldContext;
import com.ntros.event.bus.SessionEventListener;
import com.ntros.event.SessionEvent;
import com.ntros.event.SessionEventType;

public class SessionCleaner implements SessionEventListener {

    @Override
    public void onSessionEvent(SessionEvent sessionEvent) {
        ProtocolContext context = null;
        if (!sessionEvent.getEventType().equals(SessionEventType.SESSION_STARTED)) {
            context = sessionEvent.getSession().getProtocolContext();
        }

        if (context == null) {
            return;
        }

        if (context.getSessionId() != null && !context.getSessionId().isEmpty() && context.getWorldId() != null && !context.getWorldId().isEmpty()) {
            WorldContext worldContext = WorldDispatcher.getWorld(context.getWorldId());
            worldContext.engine().remove(context.getSessionId(), worldContext.state());
        }

    }
}
