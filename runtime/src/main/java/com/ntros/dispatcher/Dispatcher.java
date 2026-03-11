package com.ntros.dispatcher;

import com.ntros.lifecycle.session.Session;
import com.ntros.protocol.Message;
import java.util.Optional;

public interface Dispatcher {

  Optional<Message> dispatch(Message message, Session session);

}
