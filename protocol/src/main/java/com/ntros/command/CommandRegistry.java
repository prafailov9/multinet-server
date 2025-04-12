package com.ntros.command;

import com.ntros.command.impl.*;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ntros.command.CommandUtil.*;

public final class CommandRegistry {

    private static final Logger LOGGER = Logger.getLogger(CommandRegistry.class.getName());

    private static final Map<String, Command> COMMAND_MAP;

    static {
        COMMAND_MAP = Map.of(JOIN, new JoinCommand(),
                WELCOME, new WelcomeCommand(),
                MOVE, new MoveCommand(),
                STATE, new StateCommand(),
                DISCONNECT, new DisconnectCommand(),
                ERROR, new ErrorCommand());
    }

    public static Command get(String key) {
        Command command = COMMAND_MAP.get(key.toUpperCase());
        LOGGER.log(Level.INFO, "retrieved command: {0}", command);

        return command;
    }

}
