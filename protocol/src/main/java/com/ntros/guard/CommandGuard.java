package com.ntros.guard;

import com.ntros.model.world.protocol.Message;
import com.ntros.session.Session;

public interface CommandGuard {

  void check(Message msg, Session s);

}
