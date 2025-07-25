package com.ntros.session;


import static com.ntros.event.SessionEvent.sessionClosed;
import static com.ntros.event.SessionEvent.sessionFailed;

import com.ntros.connection.Connection;
import com.ntros.event.bus.SessionEventBus;
import com.ntros.message.ProtocolContext;
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

  private final Connection connection;
  private final ProtocolContext protocolContext;
  private final ClientMessageProcessor clientMessageProcessor;
  private final ServerMessageProcessor serverMessageProcessor;

  private final AtomicBoolean terminated = new AtomicBoolean(false);
  private final AtomicBoolean notifyOnTerminate = new AtomicBoolean(false);

  private volatile boolean running = true;


  public ClientSession(Connection connection) {
    this.connection = connection;
    this.protocolContext = new ProtocolContext();
    this.clientMessageProcessor = new RequestClientMessageProcessor();
    this.serverMessageProcessor = new ResponseServerMessageProcessor();
  }

  @Override
  public void run() {
    try {
      while (running && connection.isOpen()) {
        String rawMessage = connection.receive();
        if (rawMessage == null || rawMessage.isEmpty()) {
          continue;
        }
        if (rawMessage.equals("_TIMEOUT_")) {
          // could send PING message to client
          continue;
        }
        try {
          String serverResponse = clientMessageProcessor.process(rawMessage, protocolContext)
              .orElseThrow(() -> new RuntimeException("[ClientSession]: no response from server."));

          serverMessageProcessor.processResponse(serverResponse, this);

        } catch (Exception ex) {
          log.error("Error: {}", protocolContext.getSessionId(), ex);
          SessionEventBus.get().publish(sessionFailed(this, ex.getMessage()));
        }
      }
    } finally {
      log.info("ClientSession {} calling terminate()...", protocolContext.getSessionId());
      terminate();
    }
  }

  /**
   * sends the server response to the client.
   */
  @Override
  public void respond(String serverResponse) {
    synchronized (connection) { // sync because writing to the socket connection
      log.info("{} received server response: {}. Sending to client...\n", this, serverResponse);
      connection.send(serverResponse);
    }
  }

  @Override
  public void stop(boolean notifyOnTerminate) {
    running = false;
    this.notifyOnTerminate.set(notifyOnTerminate);
  }

  @Override
  public ProtocolContext getProtocolContext() {
    return protocolContext;
  }

  @Override
  public void terminate() {
    if (!terminated.compareAndSet(false, true)) {
      // already terminated once
      return;
    }

    running = false;
    connection.close();

    if (notifyOnTerminate.get()) {
      String serverMessage = String.format("Closing session %s...", protocolContext.getSessionId());
      log.info("publishing SESSION_CLOSED Event..." + serverMessage);
      SessionEventBus.get().publish(sessionClosed(this, serverMessage));
    }
  }
}
