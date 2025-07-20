package com.ntros.event.listener.factory;

import com.ntros.event.listener.ClientSessionEventListener;
import com.ntros.event.listener.SessionEventListener;
import com.ntros.event.listener.SessionManager;
import com.ntros.server.scheduler.WorldTickScheduler;

public class ClientSessionEventListenerFactory implements SessionEventListenerFactory {

    private final SessionManager sessionManager;
    private final WorldTickScheduler tickScheduler;

    public ClientSessionEventListenerFactory(SessionManager sessionManager, WorldTickScheduler tickScheduler) {
        this.sessionManager = sessionManager;
        this.tickScheduler = tickScheduler;
    }

    @Override
    public SessionEventListener create() {
        return new ClientSessionEventListener(sessionManager, tickScheduler);
    }
}
