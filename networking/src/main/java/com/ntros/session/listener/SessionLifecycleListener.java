package com.ntros.session.listener;

import com.ntros.session.Session;

public interface SessionLifecycleListener {

    void onSessionClosed(Session session);

}
