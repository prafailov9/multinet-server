package com.ntros.messageprocessing.client;

import com.ntros.dispatcher.Dispatcher;
import com.ntros.dispatcher.MessageDispatcher;
import com.ntros.lifecycle.session.Session;
import com.ntros.messageprocessing.NoResponseFromServerException;
import com.ntros.validation.ClientCommandValidator;
import com.ntros.validation.CommandValidator;
import com.ntros.parser.MessageParser;
import com.ntros.protocol.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestClientMessageProcessor implements ClientMessageProcessor {

  private static final String SESSION_FAILED_NOTIFIER = "SESSION_FAILED";

  private final MessageParser messageParser;
  private final CommandValidator commandValidator;
  private final Dispatcher dispatcher;

  public RequestClientMessageProcessor() {
    this.messageParser = new MessageParser();
    this.commandValidator = new ClientCommandValidator();
    this.dispatcher = new MessageDispatcher();
  }

  /**
   * Processing the raw network message from the client
   */
  @Override
  public Message process(String rawMessage, Session session) {
    if (rawMessage.startsWith(SESSION_FAILED_NOTIFIER)) {
      session.stop();
      return Message.errorMsg("unexpected session failure. Stopped session.");
    }

    Message message = messageParser.parse(rawMessage);
    commandValidator.validate(message, session);

    return dispatcher.dispatch(message, session)
        .orElseThrow(() -> new NoResponseFromServerException("Server returned empty response"));
  }

}
