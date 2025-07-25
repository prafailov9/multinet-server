package com.ntros.command;

import static com.ntros.command.CommandUtil.DISCONNECT;
import static com.ntros.command.CommandUtil.ERROR;
import static com.ntros.command.CommandUtil.JOIN;
import static com.ntros.command.CommandUtil.MOVE;
import static com.ntros.command.CommandUtil.STATE;
import static com.ntros.command.CommandUtil.WELCOME;

import com.ntros.command.impl.Command;
import com.ntros.command.impl.DisconnectCommand;
import com.ntros.command.impl.ErrorCommand;
import com.ntros.command.impl.JoinCommand;
import com.ntros.command.impl.MoveCommand;
import com.ntros.command.impl.StateCommand;
import com.ntros.command.impl.WelcomeCommand;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

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
