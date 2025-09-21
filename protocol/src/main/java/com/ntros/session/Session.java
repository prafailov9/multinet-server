package com.ntros.session;

import com.ntros.message.ProtocolContext;

/**
 * Represents an active connection between a client and the server.
 */
public interface Session {

    ProtocolContext getProtocolContext();

    /**
     * Start processing client data stream
     */
    void start();

    /**
     * Sends server response to client
     *
     * @param serverResponse - the generated server response from
     */
    void response(String serverResponse);

    // flags session for termination
    void stop();

    // ends session
    void terminate();
}
