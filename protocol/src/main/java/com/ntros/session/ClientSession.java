package com.ntros.session;


import static com.ntros.event.SessionEvent.sessionClosed;
import static com.ntros.event.SessionEvent.sessionFailed;

import com.ntros.connection.Connection;
import com.ntros.event.bus.SessionEventBus;
import com.ntros.instance.InstanceRegistry;
import com.ntros.instance.ins.Instance;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.process.ClientMessageProcessor;
import com.ntros.session.process.RequestClientMessageProcessor;
import com.ntros.session.process.ResponseServerMessageProcessor;
import com.ntros.session.process.ServerMessageProcessor;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstracts the external client, its behaviour and how it's represented in the system. Emits events
 * based on the server response.
 */
@Slf4j
public class ClientSession implements Session {

  private static final String TIMEOUT_ERROR_MESSAGE = "_TIMEOUT_";

  private final Connection connection;
  private final SessionContext sessionContext;
  private final ClientMessageProcessor clientMessageProcessor;
  private final ServerMessageProcessor serverMessageProcessor;

  private final AtomicBoolean terminated = new AtomicBoolean(false);
  private final AtomicBoolean notifyOnTerminate = new AtomicBoolean(false);

  private volatile boolean running = true;


  public ClientSession(Connection connection) {
    this.connection = connection;
    this.sessionContext = new SessionContext(IdSequenceGenerator.getInstance().nextSessionId());
    this.clientMessageProcessor = new RequestClientMessageProcessor();
    this.serverMessageProcessor = new ResponseServerMessageProcessor();
  }

  /**
   * Starts a new session for a single user(protocol context).
   */
  @Override
  public void start() {
    try {
      while (running && connection.isOpen()) {
        String rawMessage = connection.receive();
        // continue processing the stream if current captured frame is empty
        if (rawMessage == null || rawMessage.isEmpty()) {
          continue;
        }
        // continue processing the stream if connection has generated a TIMEOUT message
        if (rawMessage.equals(TIMEOUT_ERROR_MESSAGE)) {
          // could send PING message to client
          continue;
        }
        try {
          ServerResponse serverResponse = clientMessageProcessor.process(rawMessage, this);
          log.info("SESSION: Received response from server: {}", serverResponse);

          serverMessageProcessor.processResponse(serverResponse, this);
        } catch (Exception ex) {
          log.error("Error: {}", sessionContext.getSessionId(), ex);
          SessionEventBus.get().publish(sessionFailed(this, ex.getMessage()));
        }
      }
    } finally {
      log.info("ClientSession {} calling terminate()...", sessionContext.getSessionId());
      terminate();
    }
  }

  /**
   * sends the server response to the client.
   */
  @Override
  public void response(String serverResponse) {
    synchronized (connection) { // sync because writing to the socket connection
      log.info("{} received server response: {}. Sending to client...\n", this, serverResponse);
      connection.send(serverResponse);
    }
  }

  @Override
  public void stop() {
    running = false;
    this.notifyOnTerminate.set(false);
  }

  @Override
  public SessionContext getSessionContext() {
    return sessionContext;
  }

//  @Override
//  public void terminate() {
//    if (!terminated.compareAndSet(false, true)) {
//      // already terminated once
//      return;
//    }
//
//    running = false;
//    connection.close();
//
//    if (notifyOnTerminate.get()) {
//      String serverMessage = String.format("Closing session %s...", sessionContext.getSessionId());
//      log.info("publishing SESSION_CLOSED Event...{}", serverMessage);
//      SessionEventBus.get().publish(sessionClosed(this, serverMessage));
//    }
//  }

  @Override
  public void terminate() {
    if (!terminated.compareAndSet(false, true)) {
      return;
    }

    running = false;
    connection.close();

    // Inline leave logic
    try {
      SessionContext ctx = sessionContext;
      if (ctx != null && ctx.isAuthenticated()) {
        String worldId = ctx.getWorldName();
        if (worldId != null && !worldId.isEmpty()) {
          Instance inst = InstanceRegistry.get(worldId);
          if (inst != null) {
            // remove entity from the world
            WorldConnector world = WorldConnectorHolder.getWorld(worldId);
            if (world != null && ctx.getEntityId() != null) {
              world.remove(ctx.getEntityId());   // IMPORTANT: entityId, not userId
            }
            // deregister session (may auto-stop if last session and config.autostart)
            inst.removeSession(this);
          }
        }
      }
    } catch (Throwable t) {
      log.warn("Terminate cleanup failed for session {}", sessionContext.getSessionId(), t);
    }
  }

}
