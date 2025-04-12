package com.ntros.command.impl;

import com.ntros.message.ProtocolContext;
import com.ntros.model.entity.Direction;
import com.ntros.model.world.Message;
import com.ntros.model.world.WorldDispatcher;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.Result;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MoveCommand extends AbstractCommand {

    private static final Logger LOGGER = Logger.getLogger(MoveCommand.class.getName());


    @Override
    public Optional<String> execute(Message message, ProtocolContext protocolContext) {
        validateContext(protocolContext);

        WorldConnector world = WorldDispatcher.getWorld(protocolContext.getWorldId());

        LOGGER.log(Level.INFO, "retrieved world {0}", world.worldName());
        Direction direction = resolveMoveIntent(message);
        LOGGER.log(Level.INFO, "resolved world and direction: {0}, {1}", new Object[]{world.worldName(), direction.name()});
        Result result = world.storeMoveIntent(new MoveRequest(protocolContext.getSessionId(), direction));

        return handleResult(result, direction.name());
    }

    private Optional<String> handleResult(Result result, String move) {
        LOGGER.log(Level.INFO, "received result: {0}", result);
        return result.success() ? Optional.of(String.format("ACK %s\n", move)) : Optional.of(String.format("ERROR %s\n", result.reason()));
    }

    private Direction resolveMoveIntent(Message message) {
        // move has to be second argument of command.
        LOGGER.log(Level.INFO, "resolving move intent for message: {0}", message);
        String move = message.getArgs().getFirst();
        if (move == null || move.isEmpty()) {
            logAndThrow("Move cannot be empty.");
        }
        LOGGER.log(Level.INFO, "resolved move: {0}", move);
        return Direction.valueOf(move); // throws illegalArgument
    }
}
