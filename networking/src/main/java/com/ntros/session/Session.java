package com.ntros.session;

import com.ntros.message.ProtocolContext;

/**
 * Represents an active connection between a client and the server.
 */
public interface Session {

    ProtocolContext getProtocolContext();

    void run();
    void send(String serverResponse);
    void terminate();
}
