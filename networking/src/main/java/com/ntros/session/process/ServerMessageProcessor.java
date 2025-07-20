package com.ntros.session.process;

import com.ntros.session.Session;

public interface ServerMessageProcessor {

    void processResponse(String serverResponse, Session session);

}
