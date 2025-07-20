package com.ntros.event.listener;

import com.ntros.event.SessionEvent;
import com.ntros.event.SessionEventType;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.WorldDispatcher;
import com.ntros.model.world.connector.WorldConnector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionCleaner implements SessionEventListener {

    @Override
    public void onSessionEvent(SessionEvent sessionEvent) {
        log.info("IN SESSION CLEANER. NO IMPLEMENTED PROCESSING OF SESSION CLOSED EVENT");
//        ProtocolContext context = null;
//        if (!sessionEvent.getEventType().equals(SessionEventType.SESSION_STARTED)) {
//            context = sessionEvent.getSession().getProtocolContext();
//        }

//        if (context == null) {
//            return;
//        }

//        if (context.getSessionId() != null && context.getWorldId() != null && !context.getWorldId().isEmpty()) {
//            WorldConnector worldConnector = WorldDispatcher.getWorld(context.getWorldId());
//            worldConnector.remove(context.getPlayerId());
//        }

    }
}
