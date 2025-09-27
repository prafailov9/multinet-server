package com.ntros.session;


import com.ntros.connection.Connection;
import com.ntros.instance.InstanceRegistry;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.session.process.ClientMessageProcessor;
import com.ntros.session.process.RequestClientMessageProcessor;
import com.ntros.session.process.ResponseServerMessageProcessor;
import com.ntros.session.process.ServerMessageProcessor;
import java.util.concurrent.TimeUnit;
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
          ServerResponse failedResponse = clientMessageProcessor.process("SESSION_FAILED", this);
          log.error("Processed session_failed response: {}", failedResponse);
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
  }

  @Override
  public SessionContext getSessionContext() {
    return sessionContext;
  }

  @Override
  public void terminate() {
    if (!terminated.compareAndSet(false, true)) {
      return;
    }

    running = false;

    if (sessionContext != null && sessionContext.isAuthenticated()
        && sessionContext.getWorldName() != null) {
      try {
        var instance = InstanceRegistry.getInstance(sessionContext.getWorldName());
        if (instance != null) {
          // queue entity removal + deregister session on the actor thread
          instance.leaveAsync(this).exceptionally(ex -> {
            log.warn("leaveAsync failed: {}", ex.toString());
            return null;
          });
        }
      } catch (Throwable t) {
        log.warn("terminate(): cleanup error: {}", t.toString());
      }
    }

    connection.close();
  }


}
