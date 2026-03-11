package com.ntros.lifecycle.session.process;

import com.ntros.dispatcher.Dispatcher;
import com.ntros.dispatcher.MessageDispatcher;
import com.ntros.lifecycle.session.Session;
import com.ntros.parser.MessageParser;
import com.ntros.protocol.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestClientMessageProcessor implements ClientMessageProcessor {

  private static final String SESSION_FAILED_NOTIFIER = "SESSION_FAILED";

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
  public Message process(String rawMessage, Session session) {
    if (rawMessage.startsWith(SESSION_FAILED_NOTIFIER)) {
      session.stop();
      return Message.errorMsg("unexpected session failure. Removed session.");
    }

    Message message = messageParser.parse(rawMessage);
    log.info("Message received: {}", message);
    return dispatcher.dispatch(message, session)
        .orElseThrow(() -> new NoResponseFromServerException("Server returned empty response"));
  }

}
