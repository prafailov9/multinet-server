package com.ntros.session;

import com.ntros.message.SessionContext;

/**
 * Represents an active connection between a client and the server.
 */
public interface Session {

    SessionContext getSessionContext();

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
