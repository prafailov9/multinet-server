package com.ntros.server;


import com.ntros.event.bus.EventBus;

import java.io.IOException;

public interface Server {

    void start(int port, EventBus eventBus) throws IOException;
    void stop() throws IOException;

}
