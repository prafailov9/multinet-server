package com.ntros.session.process;

import com.ntros.dispatcher.Dispatcher;
import com.ntros.dispatcher.MessageDispatcher;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.parser.MessageParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestClientMessageProcessor implements ClientMessageProcessor {

    private final MessageParser messageParser;
    private final Dispatcher dispatcher;

    public RequestClientMessageProcessor() {
        this.messageParser = new MessageParser();
        this.dispatcher = new MessageDispatcher();
    }

    /** Processing the raw network message from the client */
    @Override
    public String process(String rawMessage, ProtocolContext protocolContext) {
        Message message = messageParser.parse(rawMessage);
        log.info("Message received: {}", message);

        ServerResponse serverResponse = dispatcher.dispatch(message, protocolContext)
                .orElseThrow(() -> new NoResponseFromServerException("Server returned empty response"));

        return serverResponse.serverMessage().toString();
    }
}
