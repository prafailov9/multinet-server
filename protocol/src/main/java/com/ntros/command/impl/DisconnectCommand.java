package com.ntros.command.impl;

import com.ntros.model.world.Message;
import com.ntros.message.ProtocolContext;

import java.util.Optional;

public class DisconnectCommand implements Command {

    @Override
    public Optional<String> execute(Message message, ProtocolContext protocolContext) {
        return Optional.empty();
    }
}
