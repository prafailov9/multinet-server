package com.ntros.event.listener;

import com.ntros.event.SessionEvent;
import com.ntros.server.scheduler.WorldTickScheduler;
import com.ntros.session.Session;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ClientSessionEventListener implements SessionEventListener {

    private final SessionManager sessionManager;
    private final WorldTickScheduler tickScheduler;
    private final AtomicBoolean schedulerRunning = new AtomicBoolean(false);

    public ClientSessionEventListener(SessionManager sessionManager, WorldTickScheduler tickScheduler) {
        this.sessionManager = sessionManager;
        this.tickScheduler = tickScheduler;
    }

    @Override
    public void onSessionEvent(SessionEvent sessionEvent) {
        switch (sessionEvent.getEventType()) {
            case SESSION_STARTED -> created(sessionEvent.getSession(), sessionEvent.getServerMessage());
            case SESSION_CLOSED -> destroyed(sessionEvent.getSession());
        }
    }

    private void created(Session session, String serverWelcomeMessage) {
        // indicates a successful JOIN command
        sessionManager.register(session);
        // send welcome response to client to trigger UI changes
        session.respond(serverWelcomeMessage);
        if (sessionManager.activeSessions() > 0 && !schedulerRunning.get()) {
            tickScheduler.start();
            schedulerRunning.set(true);
        }
    }

    private void destroyed(Session session) {
        sessionManager.remove(session);
        // Check if there are any remaining active sessions.
        if (sessionManager.activeSessions() == 0 && schedulerRunning.get()) {
            tickScheduler.stop();
            schedulerRunning.set(false);
            log.info("Last session was destroyed. TickScheduler stopped.");
        }
    }
}
