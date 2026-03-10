package com.ntros.dispatcher;

import com.ntros.protocol.Message;
import com.ntros.protocol.response.ServerResponse;
import com.ntros.lifecycle.session.Session;
import java.util.Optional;

public interface Dispatcher {

  Optional<ServerResponse> dispatch(Message message, Session session);

}
