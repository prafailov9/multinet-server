package com.ntros.lifecycle.session.process;

import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.lifecycle.session.Session;

public interface ServerMessageProcessor {

    void processResponse(ServerResponse serverResponse, Session session);

}
