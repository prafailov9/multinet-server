package com.ntros.session;

import com.ntros.message.ProtocolContext;

/**
 * Represents an active connection between a client and the server.
 */
public interface Session {

    ProtocolContext getProtocolContext();
    // sends client request to server
    void run();
    // sends server response to client
    void respond(String serverResponse);

    // flags session for termination
    void stop(boolean notifyOnTerminate);

    // ends session
    void terminate();
}
