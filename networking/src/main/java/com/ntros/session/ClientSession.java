package com.ntros.session;


import com.ntros.connection.Connection;
import com.ntros.dispatcher.Dispatcher;
import com.ntros.dispatcher.MessageDispatcher;
import com.ntros.event.bus.SessionEventBus;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.Message;
import com.ntros.parser.MessageParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ntros.event.SessionEvent.*;

/**
 * Abstracts the external client, its behaviour and how it's represented in the system
 */
@Slf4j
public class ClientSession implements Session {

    private final Connection connection;
    private final MessageParser messageParser;
    private final ProtocolContext protocolContext;
    private final Dispatcher dispatcher;
    private final AtomicBoolean terminated = new AtomicBoolean(false);
    private volatile boolean running = true;

    public ClientSession(Connection connection) {
        this.connection = connection;
        this.messageParser = new MessageParser();
        this.protocolContext = new ProtocolContext();
        this.dispatcher = new MessageDispatcher();
    }

    @Override
    public void run() {
        try {
            while (running && connection.isOpen()) {
                String data = connection.receive();
                if (data == null || data.isEmpty()) {
                    continue;
                }

                try {
                    Message message = messageParser.parse(data);
                    String serverResponse = dispatcher.dispatch(message, protocolContext)
                            .orElseThrow(() -> new RuntimeException("[ClientSession]: no response from server."));

                    if (protocolContext.isAuthenticated() && serverResponse.startsWith("WELCOME")) {
                        SessionEventBus.get().publish(sessionStarted(this, "Starting client session...", serverResponse));
                    }

                } catch (Exception ex) {
                    log.error("Error: {}", protocolContext.getSessionId(), ex);
                    SessionEventBus.get().publish(sessionFailed(this, ex.getMessage()));
                }
            }
        } finally {
            log.info("ClientSession calling terminate()...");
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
    public void stop() {
        running = false;
    }

    @Override
    public ProtocolContext getProtocolContext() {
        return protocolContext;
    }

    @Override
    public void terminate() {
        log.info(">>> [IN CLIENT_SESSION TERMINATE]: Called from: {}", Arrays.toString(Thread.currentThread().getStackTrace()));
        if (!terminated.compareAndSet(false, true)) {
            log.info("[IN CLIENT_SESSION TERMINATE]: Session {} already terminated. Returning...", protocolContext);
            // already terminated once
            return;
        }

        running = false;
        connection.close();

        String serverMessage = String.format("Closing session %s...", protocolContext.getSessionId());
        log.info("[IN CLIENT_SESSION TERMINATE]: publishing SESSION_CLOSED Event..." + serverMessage);
        SessionEventBus.get().publish(sessionClosed(this, serverMessage));
    }

}
