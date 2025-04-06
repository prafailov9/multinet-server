package com.ntros.dispatcher;

import com.ntros.command.CommandRegistry;
import com.ntros.command.impl.Command;
import com.ntros.command.impl.ErrorCommand;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.Message;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageDispatcher implements Dispatcher {
    private static final Logger LOGGER = Logger.getLogger(MessageDispatcher.class.getName());


    @Override
    public Optional<String> dispatch(Message message, ProtocolContext protocolContext) {
        LOGGER.log(Level.INFO, "received message: {0}", message);
        Command command = getCommand(message.getCommand().name());

        return command.execute(message, protocolContext);
    }


    private Command getCommand(String commandName) {
        Command command = CommandRegistry.get(commandName);
        return (command == null) ? new ErrorCommand() : command;

    }

}
