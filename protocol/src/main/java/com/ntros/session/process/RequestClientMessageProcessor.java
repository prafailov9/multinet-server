package com.ntros.session.process;

import com.ntros.dispatcher.Dispatcher;
import com.ntros.dispatcher.MessageDispatcher;
import com.ntros.instance.InstanceRegistry;
import com.ntros.instance.ins.Instance;
import com.ntros.message.SessionContext;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.CommandType;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.parser.MessageParser;
import com.ntros.session.Session;
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
      removeSessionEntityFromWorld(session);
      return ServerResponse.ofError(
          new Message(CommandType.ERROR, List.of("unexpected session failure. Removed session.")),
          CommandResult.failed(session.getSessionContext().getUserId(),
              session.getSessionContext().getWorldName(), "Session failed")
      );
    }

    Message message = messageParser.parse(rawMessage);
    log.info("Message received: {}", message);
    return dispatcher.dispatch(message, session)
        .orElseThrow(() -> new NoResponseFromServerException("Server returned empty response"));
  }

  private void removeSessionEntityFromWorld(Session session) {
    SessionContext ctx = session.getSessionContext();
    if (ctx == null || !ctx.isAuthenticated()) {
      log.warn("IN MESSAGE PROCESSOR: removeEntity from World: sessionContext is invalid: {}. ",
          ctx);
      return;
    }

    String worldName = ctx.getWorldName();
    if (ctx.getSessionId() >= 0 && worldName != null && !worldName.isEmpty()) {
      Instance instance = InstanceRegistry.getInstance(worldName);
      log.info("IN EVENT_LISTENER: Removing entity {} from world {}. ", ctx, worldName);
      instance.removeEntity(ctx.getEntityId());
      instance.removeSession(session);
    }
  }

}
