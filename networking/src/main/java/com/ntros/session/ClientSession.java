package com.ntros.session;


import com.ntros.connection.Connection;
import com.ntros.connection.ConnectionReceiveException;
import com.ntros.dispatcher.Dispatcher;
import com.ntros.dispatcher.MessageDispatcher;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.Message;
import com.ntros.parser.MessageParser;
import com.ntros.event.SessionEventType;
import com.ntros.event.SessionEvent;
import com.ntros.event.bus.EventBus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns and uses Connection to receive/send messages
 */
public class ClientSession implements Session {

    private static final Logger LOGGER = Logger.getLogger(ClientSession.class.getName());

    // session-owned, connection.receive() no longer blocks server's executor
    private final ExecutorService readExecutor = Executors.newSingleThreadExecutor();
    private final Connection connection;
    private final MessageParser messageParser;
    private final ProtocolContext protocolContext;
    private final Dispatcher dispatcher;
    private final EventBus eventBus;
    private volatile boolean running = true;

    public ClientSession(Connection connection, EventBus eventBus) {
        this.connection = connection;
        this.messageParser = new MessageParser();
        this.protocolContext = new ProtocolContext();
        this.dispatcher = new MessageDispatcher();
        this.eventBus = eventBus;
    }

    @Override
    public void run() {
        SessionEvent event = new SessionEvent(SessionEventType.SESSION_STARTED, this, "starting client session...");
        LOGGER.log(Level.INFO, "Session started. Sending event: " + event.toString());
        eventBus.publish(event);
        readExecutor.submit(this::readAndExecute);
    }

    /**
     * will read the socket stream and run based on sent commands.
     */
    private void readAndExecute() {
        try {
            while (running && connection.isOpen()) {
                String data = readFromConnection();

                if (data == null || data.isEmpty()) {
                    continue;
                }
                try {
                    Message message = messageParser.parse(data);
                    String serverResponse = dispatcher.dispatch(message, protocolContext)
                            .orElseThrow(() -> new RuntimeException("[ClientSession]: no response from server."));

                    connection.send(serverResponse);
                } catch (RuntimeException ex) {
                    LOGGER.log(Level.SEVERE, "Error: {0}, {1}", new Object[]{protocolContext.getSessionId(), ex});
                    eventBus.publish(new SessionEvent(SessionEventType.SESSION_FAILED, this, ex.getMessage()));
                }
            }
        } finally {
            terminate();
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
        readExecutor.shutdownNow();
        connection.close();
        eventBus.publish(new SessionEvent(SessionEventType.SESSION_CLOSED, this, String.format("Closing %s session...", protocolContext.getSessionId())));
    }

    /**
     * sends the server response to the client.
     */
    @Override
    public void send(String serverResponse) {
        synchronized (connection) { // sync because writing to the socket connection
            LOGGER.log(Level.INFO, "{0} received server response: \n" + serverResponse);
            connection.send(serverResponse);
        }
    }

    private String readFromConnection() {
        try {
            return connection.receive();
        } catch (ConnectionReceiveException ex) {
            LOGGER.log(Level.SEVERE, "[ClientSession]: Error during reading socket data: {}", ex.getMessage());
            return null;
        }
    }

}
