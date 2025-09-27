package com.ntros.model.world.protocol;

public record CommandResult(boolean success, String playerName, String worldName, String reason) {

  public static CommandResult succeeded(String playerName, String worldName, String reason) {
    return new CommandResult(true, playerName, worldName, reason);
  }

  public static CommandResult failed(String playerName, String worldName, String reason) {
    return new CommandResult(false, playerName, worldName, reason);
  }

}
