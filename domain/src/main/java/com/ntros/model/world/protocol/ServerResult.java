package com.ntros.model.world.protocol;

public record ServerResult(boolean success, String playerName, String worldName, String reason) {

  public static ServerResult succeeded(String playerName, String worldName, String reason) {
    return new ServerResult(true, playerName, worldName, reason);
  }

  public static ServerResult failed(String playerName, String worldName, String reason) {
    return new ServerResult(false, playerName, worldName, reason);
  }

}
