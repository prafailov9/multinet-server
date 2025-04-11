package com.ntros.event.listener;

import com.ntros.server.scheduler.WorldTickScheduler;
import com.ntros.event.bus.SessionEventListener;
import com.ntros.event.SessionEvent;

public class ScheduledEventListener implements SessionEventListener {

    private final WorldTickScheduler worldTickScheduler = new WorldTickScheduler();


    @Override
    public void onSessionEvent(SessionEvent sessionEvent) {

    }
}
