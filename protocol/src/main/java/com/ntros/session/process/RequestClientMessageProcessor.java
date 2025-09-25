package com.ntros.session.process;

import com.ntros.dispatcher.Dispatcher;
import com.ntros.dispatcher.MessageDispatcher;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.parser.MessageParser;
import com.ntros.session.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestClientMessageProcessor implements ClientMessageProcessor {

  private final MessageParser messageParser;
  private final Dispatcher dispatcher;

  public RequestClientMessageProcessor() {
    this.messageParser = new MessageParser();
    this.dispatcher = new MessageDispatcher();
  }

  /**
   * Processing the raw network message from the client
   */
  @Override
  public ServerResponse process(String rawMessage, Session session) {
    Message message = messageParser.parse(rawMessage);
    log.info("Message received: {}", message);
    return dispatcher.dispatch(message, session)
        .orElseThrow(() -> new NoResponseFromServerException("Server returned empty response"));
  }
}
