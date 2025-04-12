package com.ntros.command.impl;

import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.*;
import com.ntros.message.ProtocolContext;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.Result;

import java.time.OffsetDateTime;
import java.util.Optional;

public class JoinCommand extends AbstractCommand {

    @Override
    public Optional<String> execute(Message message, ProtocolContext protocolContext) {
        String playerName = resolvePlayer(message);
        WorldConnector world = resolveWorld(message);
        Result result = world.add(new JoinRequest(playerName));

        // return server command
        return handleResult(result, protocolContext);
    }

    protected String resolvePlayer(Message message) {
        String playerName = message.getArgs().getFirst();
        if (playerName == null || playerName.isEmpty()) {
            logAndThrow("[JOIN Command]: no player name given.");
        }

        return playerName;
    }

    protected WorldConnector resolveWorld(Message message) {
        return message.getArgs().getLast().startsWith("world")
                ? WorldDispatcher.getWorld(message.getArgs().getLast()) // only works for Move requests
                : WorldDispatcher.getDefaultWorld();
    }

    private Optional<String> handleResult(Result result, ProtocolContext protocolContext) {
        if (result.success()) {
            protocolContext.setSessionId(IdSequenceGenerator.getInstance().getNextSessionId());
            protocolContext.setPlayerId(result.playerName());
            protocolContext.setWorldId(result.worldName());
            protocolContext.setJoinedAt(OffsetDateTime.now());
            protocolContext.setAuthenticated(true);
            System.out.println("[JOIN Command]: success. Sending WELCOME response to client: " + protocolContext);
            return Optional.of(String.format("%s %s\n", CommandType.WELCOME.name(), result.playerName()));
        }
        protocolContext.setAuthenticated(false);
        String err = String.format("%s %s\n", CommandType.ERROR.name(), result.reason());
        System.out.println("[JOIN Command]: failure. Sending ERROR response: " + err);
        return Optional.of(err);
    }


}
