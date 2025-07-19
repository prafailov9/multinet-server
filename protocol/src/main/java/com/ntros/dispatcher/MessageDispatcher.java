package com.ntros.dispatcher;

import com.ntros.command.CommandRegistry;
import com.ntros.command.impl.Command;
import com.ntros.command.impl.ErrorCommand;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MessageDispatcher implements Dispatcher {


    @Override
    public Optional<String> dispatch(Message message, ProtocolContext protocolContext) {
        log.info("received message: {}", message);
        Command command = getCommand(message.command().name());

        return command.execute(message, protocolContext);
    }


    private Command getCommand(String commandName) {
        Command command = CommandRegistry.get(commandName);
        return (command == null) ? new ErrorCommand() : command;

    }

}
