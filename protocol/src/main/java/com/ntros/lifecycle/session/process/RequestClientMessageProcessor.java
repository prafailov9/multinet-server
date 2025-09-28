package com.ntros.lifecycle.session.process;

import com.ntros.dispatcher.Dispatcher;
import com.ntros.dispatcher.MessageDispatcher;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.instance.Instance;
import com.ntros.message.SessionContext;
import com.ntros.model.world.protocol.CommandType;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.parser.MessageParser;
import com.ntros.lifecycle.session.Session;
import java.util.List;
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
  public ServerResponse process(String rawMessage, Session session) {
    if (rawMessage.startsWith(SESSION_FAILED_NOTIFIER)) {
//      removeSessionEntityFromWorld(session);
      session.stop();
      return ServerResponse.ofError(
          new Message(CommandType.ERROR, List.of("unexpected session failure. Removed session.")),
          CommandResult.failed(session.getSessionContext().getUserId(),
              session.getSessionContext().getWorldName(), "Session failed"));
    }

    Message message = messageParser.parse(rawMessage);
    log.info("Message received: {}", message);
    return dispatcher.dispatch(message, session)
        .orElseThrow(() -> new NoResponseFromServerException("Server returned empty response"));
  }

  private void removeSessionEntityFromWorld(Session session) {
    SessionContext ctx = session.getSessionContext();
    if (ctx == null || !ctx.isAuthenticated()) {
      log.warn("SessionContext is invalid: {}. Exiting...", ctx);
      return;
    }

    String worldName = ctx.getWorldName();
    if (ctx.getSessionId() >= 0 && worldName != null && !worldName.isEmpty()) {
      Instance instance = Instances.getInstance(worldName);
      log.info("IN MESSAGE PROCESSOR:: Removing entity {} from world {}. ", ctx, worldName);
      instance.leaveAsync(session);
    }
  }

}
