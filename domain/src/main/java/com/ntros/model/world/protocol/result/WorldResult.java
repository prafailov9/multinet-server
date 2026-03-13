package com.ntros.model.world.protocol.result;

public record WorldResult(boolean success, String playerName, String worldName, String reason) {

  public static WorldResult succeeded(String playerName, String worldName, String reason) {
    return new WorldResult(true, playerName, worldName, reason);
  }

  public static WorldResult failed(String playerName, String worldName, String reason) {
    return new WorldResult(false, playerName, worldName, reason);
  }

}
