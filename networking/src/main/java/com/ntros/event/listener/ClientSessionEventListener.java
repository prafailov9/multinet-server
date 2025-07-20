package com.ntros.event.listener;

import com.ntros.event.SessionEvent;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.WorldDispatcher;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.server.scheduler.WorldTickScheduler;
import com.ntros.session.Session;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ClientSessionEventListener implements SessionEventListener {

    private final SessionManager sessionManager;
    private final WorldTickScheduler tickScheduler;
    private final AtomicBoolean tickSchedulerRunning = new AtomicBoolean(false);

    public ClientSessionEventListener(SessionManager sessionManager, WorldTickScheduler tickScheduler) {
        this.sessionManager = sessionManager;
        this.tickScheduler = tickScheduler;
    }

    @Override
    public void onSessionEvent(SessionEvent sessionEvent) {
        switch (sessionEvent.getEventType()) {
            case SESSION_STARTED -> started(sessionEvent.getSession(), sessionEvent.getServerMessage());
            case SESSION_CLOSED -> closed(sessionEvent.getSession());
            case SESSION_FAILED -> failed(sessionEvent.getSession());
        }
    }

    /**
     * Registers the current session.
     * Starts the Tick Server on first registered session.
     * @param session
     * @param serverWelcomeMessage
     */
    private void started(Session session, String serverWelcomeMessage) {
        // indicates a successful JOIN command
        sessionManager.register(session);
        // send welcome response to client to trigger UI changes
        session.respond(serverWelcomeMessage);
        if (sessionManager.activeSessions() - 1 == 0 && !tickSchedulerRunning.get()) {
            tickScheduler.start();
            tickSchedulerRunning.set(true);
        }
    }

    private void closed(Session session) {
        log.info("[EVENT_LISTENER]: Received SESSION_CLOSED Event. Closing {}.", session.getProtocolContext());
        sessionManager.remove(session);
        removeSessionEntityFromWorld(session.getProtocolContext());
        closeAll();
    }

    private void failed(Session session) {
        log.info("[EVENT_LISTENER]: Received SESSION_FAILED Event. Closing {}.", session.getProtocolContext());
        sessionManager.remove(session);

        removeSessionEntityFromWorld(session.getProtocolContext());
        closeAll();
    }

    /**
     * stops session and tick scheduler
     */
    private void closeAll() {
        // Check if there are any remaining active sessions.
        if (sessionManager.activeSessions() == 0 && tickSchedulerRunning.get()) {
            tickScheduler.stop();
            tickSchedulerRunning.set(false);
            log.info("Last session was closed. TickScheduler stopped.");
        }
    }

    private void removeSessionEntityFromWorld(ProtocolContext context) {
        if (context == null || !context.isAuthenticated()) {
            log.warn("IN EVENT_LISTENER- removeEntity from World: sessionContext is invalid: {}. ", context);
            return;
        }
        if (context.getSessionId() != null && context.getWorldId() != null && !context.getWorldId().isEmpty()) {
            WorldConnector worldConnector = WorldDispatcher.getWorld(context.getWorldId());
            log.info("IN EVENT_LISTENER: Removing entity {} from world {}. ", context, worldConnector.worldName());
            worldConnector.remove(context.getPlayerId());
        }
    }

}
