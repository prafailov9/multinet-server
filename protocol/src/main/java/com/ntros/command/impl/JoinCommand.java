package com.ntros.command.impl;

import com.ntros.message.ProtocolContext;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.CommandType;
import com.ntros.model.world.Message;
import com.ntros.model.world.WorldDispatcher;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.Result;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
        String playerName = message.args().getFirst();
        if (playerName == null || playerName.isEmpty()) {
            logAndThrow("[JOIN Command]: no player name given.");
        }

        return playerName;
    }

    protected WorldConnector resolveWorld(Message message) {
        log.info("[JOIN COMMAND]: All worlds: {}", WorldDispatcher.getAllWorlds().stream().map(WorldConnector::worldName).collect(Collectors.toList()));
        List<String> args = message.args();

        if (args.size() >= 2) {
            String worldName = args.get(1);
            WorldConnector world = WorldDispatcher.getWorld(worldName);
            if (world != null) return world;
            log.warn("[JOIN COMMAND]: Unknown world '{}', falling back to default", worldName);
        }

        return WorldDispatcher.getDefaultWorld();
    }

    private Optional<String> handleResult(Result result, ProtocolContext protocolContext) {
        if (result.success()) {
            protocolContext.setSessionId(IdSequenceGenerator.getInstance().getNextSessionId());
            protocolContext.setPlayerId(result.playerName());
            protocolContext.setWorldId(result.worldName());
            protocolContext.setJoinedAt(OffsetDateTime.now());
            protocolContext.setAuthenticated(true);
            log.info("[JOIN Command]: success. Sending WELCOME response to client: {}", protocolContext);
            return Optional.of(String.format("%s %s\n", CommandType.WELCOME.name(), result.playerName()));
        }
        protocolContext.setAuthenticated(false);
        String err = String.format("%s %s\n", CommandType.ERROR.name(), result.reason());
        log.error("[JOIN Command]: failure. Sending ERROR response: {}", err);
        return Optional.of(err);
    }


}
