package com.ntros.command;

import com.ntros.model.world.protocol.CommandType;
import java.util.List;

public class CommandUtil {

  public static final List<CommandType> COMMANDS = List.of(CommandType.values());
  public static final List<CommandType> SERVER_COMMANDS = List.of(CommandType.ERROR,
      CommandType.STATE, CommandType.DISCONNECT, CommandType.WELCOME);
  public static final List<CommandType> CLIENT_COMMANDS = List.of(CommandType.JOIN,
      CommandType.MOVE, CommandType.DISCONNECT);

  public static final String JOIN = "JOIN";
  public static final String MOVE = "MOVE";
  public static final String DISCONNECT = "DISCONNECT";
  public static final String STATE = "STATE";
  public static final String WELCOME = "WELCOME";
  public static final String ERROR = "ERROR";

}
