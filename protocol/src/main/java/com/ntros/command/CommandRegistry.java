package com.ntros.command;

import com.ntros.command.impl.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.logging.Level;

import static com.ntros.command.CommandUtil.*;

@Slf4j
public final class CommandRegistry {
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
        log.info("retrieved command: {}", key);
        return command;
    }

}
