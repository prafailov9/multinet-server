package com.ntros.session;

import com.ntros.dispatcher.Dispatcher;
import com.ntros.dispatcher.MessageDispatcher;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.Message;
import com.ntros.parser.MessageParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SessionProtocolHandler implements ProtocolHandler {

    private final MessageParser messageParser;
    private final Dispatcher dispatcher;

    public SessionProtocolHandler() {
        this.messageParser = new MessageParser();
        this.dispatcher = new MessageDispatcher();
    }



    public Optional<String> handle(String data, ProtocolContext ctx) {
        try {
            Message message = messageParser.parse(data);
            return dispatcher.dispatch(message, ctx);
        } catch (Exception e) {
            log.error("[SessionMessageHandler] Failed to handle message: {}", data, e);
            return Optional.empty();
        }
    }
}
