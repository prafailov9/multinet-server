package com.ntros.command.utils;

import static com.ntros.command.utils.CommandUtil.AUTH;
import static com.ntros.command.utils.CommandUtil.DISCONNECT;
import static com.ntros.command.utils.CommandUtil.JOIN;
import static com.ntros.command.utils.CommandUtil.MOVE;

import com.ntros.command.AuthCommand;
import com.ntros.command.Command;
import com.ntros.command.DisconnectCommand;
import com.ntros.command.JoinCommand;
import com.ntros.command.MoveCommand;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CommandRegistry {

  private static final Map<String, Command> COMMAND_MAP;

  static {
    COMMAND_MAP = Map.of(JOIN, new JoinCommand(),
        AUTH, new AuthCommand(),
        MOVE, new MoveCommand(),
        DISCONNECT, new DisconnectCommand());
  }

  public static Command get(String key) {
    Command command = COMMAND_MAP.get(key.toUpperCase());
    log.info("retrieved command: {}", key);
    return command;
  }

}
