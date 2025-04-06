package com.ntros.command.impl;

import com.ntros.model.world.WorldRegistry;
import com.ntros.model.world.Message;
import com.ntros.model.world.WorldContext;

import static com.ntros.model.world.WorldRegistry.getGridWorld;
import static com.ntros.model.world.WorldRegistry.getRandomGridWorld;

public abstract class AbstractCommand implements Command {

    protected String resolvePlayer(Message message) {
        String playerName = message.getArgs().getFirst();
        if (playerName == null || playerName.isEmpty()) {
            String err = "[Abstract Command]: no player name given.";
            System.err.println(err);
            throw new RuntimeException(err);
        }

        return playerName;
    }

    protected WorldContext resolveWorld(Message message) {
        return message.getArgs().getLast().startsWith("world")
                ? getGridWorld(message.getArgs().getLast())
                : getRandomGridWorld();
//        String worldName = message.getArgs().get(2);
//        WorldContext world = null;
//        if (worldName == null || worldName.isEmpty()) {
//            world = WorldRegistry.getRandomGridWorld();
//        }
//        if (world == null) {
//            world = WorldRegistry.getGridWorld(worldName);
//        }
//
//        if (!world.isFree()) {
//            throw new RuntimeException("No open positions on selected world: " + world);
//        }
//        return world;
    }

}
