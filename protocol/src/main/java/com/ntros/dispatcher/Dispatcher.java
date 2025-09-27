package com.ntros.dispatcher;

import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.session.Session;
import java.util.Optional;

public interface Dispatcher {

  Optional<ServerResponse> dispatch(Message message, Session session);

}
