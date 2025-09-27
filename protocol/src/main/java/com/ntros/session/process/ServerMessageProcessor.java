package com.ntros.session.process;

import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.session.Session;

public interface ServerMessageProcessor {

    void processResponse(ServerResponse serverResponse, Session session);

}
