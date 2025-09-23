package com.ntros.session.process;

import com.ntros.session.Session;

public interface ClientMessageProcessor {

  String process(String rawMessage, Session session);

}
