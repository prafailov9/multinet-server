package com.ntros.command.impl;

import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.lifecycle.session.Session;
import java.util.Optional;

public interface Command {

  Optional<ServerResponse> execute(Message message, Session session);

}
