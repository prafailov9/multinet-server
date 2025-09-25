package com.ntros.session;

import com.ntros.message.ClientProfile;

/**
 * Represents an active connection between a client and the server.
 */
public interface Session {

    ClientProfile getProtocolContext();

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
