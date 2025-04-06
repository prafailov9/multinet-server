package com.ntros.server;

import com.ntros.message.ProtocolContext;
import com.ntros.session.event.EventBus;

import java.io.IOException;

public interface Server {

    void start(int port, EventBus eventBus) throws IOException;
    void stop() throws IOException;

}
