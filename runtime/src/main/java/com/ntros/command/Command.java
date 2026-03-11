package com.ntros.command;

import com.ntros.lifecycle.session.Session;
import com.ntros.protocol.Message;

public interface Command {

  Message execute(Message message, Session session);

}
