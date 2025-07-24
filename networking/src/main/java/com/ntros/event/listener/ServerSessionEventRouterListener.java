package com.ntros.event.listener;

import com.ntros.event.SessionEvent;
import com.ntros.server.scheduler.TickScheduler;

import java.util.ArrayList;
import java.util.List;

public class ServerSessionEventRouterListener implements SessionEventListener {

    private final List<SessionManagerTickSchedulerEntry> sessionManagerTickSchedulerEntries;

    public ServerSessionEventRouterListener() {
        this.sessionManagerTickSchedulerEntries = new ArrayList<>();
    }

    public void register(SessionManager sessionManager, TickScheduler tickScheduler) {
        sessionManagerTickSchedulerEntries.add(new SessionManagerTickSchedulerEntry(sessionManager, tickScheduler));
    }
    @Override
    public void onSessionEvent(SessionEvent sessionEvent) {
        for (var entry: sessionManagerTickSchedulerEntries) {
//            entry.sessionManager.getActiveSessions().stream().map(x -> x.getProtocolContext().getSessionId().equals(sessionEvent.getSessionId()))
        }
    }


    private record SessionManagerTickSchedulerEntry(SessionManager sessionManager, TickScheduler tickScheduler) {
    }

}
