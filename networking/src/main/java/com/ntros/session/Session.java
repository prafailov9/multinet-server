package com.ntros.session;

import com.ntros.message.ProtocolContext;

/**
 * Represents an active connection between a client and the server.
 */
public interface Session {

    ProtocolContext getProtocolContext();
    // sends client request to server
    void send();
    // sends server response to client
    void accept(String serverResponse);
    // ends session
    void terminate();
}
