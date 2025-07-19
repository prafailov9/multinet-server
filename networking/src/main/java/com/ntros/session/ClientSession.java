package com.ntros.session;


import com.ntros.connection.Connection;
import com.ntros.dispatcher.Dispatcher;
import com.ntros.dispatcher.MessageDispatcher;
import com.ntros.event.SessionEvent;
import com.ntros.event.bus.SessionEventBus;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.Message;
import com.ntros.parser.MessageParser;
import lombok.extern.slf4j.Slf4j;

import static com.ntros.event.SessionEvent.sessionClosed;

/**
 * Abstracts the external client, its behaviour and how it's represented in the system
 */
@Slf4j
public class ClientSession implements Session {

    private final Connection connection;
    private final MessageParser messageParser;
    private final ProtocolContext protocolContext;
    private final Dispatcher dispatcher;
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

                    if (protocolContext.isAuthenticated()) {
                        respond(serverResponse);

                        // TODO: move event logic to protocol layer
//                    SessionEvent sessionEvent = SessionEvent.ofSessionStarted(this, "starting client session...", serverResponse);
//                        log.info("Session started. Sending event: {}", sessionEvent);
//                        eventBus.publish(sessionEvent);
                    }

                } catch (Exception ex) {
                    log.error("Error: {}", protocolContext.getSessionId(), ex);
                    SessionEventBus.get().publish(SessionEvent.sessionFailed(this, ex.getMessage()));

                }
            }
        } finally {
            terminate();
        }
    }

    /**
     * sends the server response to the client.
     */
    @Override
    public void respond(String serverResponse) {
        synchronized (connection) { // sync because writing to the socket connection
            log.info("{} received server response: {}\n", this, serverResponse);
            connection.send(serverResponse);
        }
    }

    @Override
    public ProtocolContext getProtocolContext() {
        return protocolContext;
    }

    @Override
    public void terminate() {
        if (!running) return;

        running = false;
        connection.close();

        String serverMessage = String.format("Closing %s session...", protocolContext.getSessionId());
        log.info(serverMessage);
        SessionEventBus.get().publish(sessionClosed(this, serverMessage));
//        respond(serverMessage);
    }

}
