package com.ntros.lifecycle.instance.actor;

public final class Actors {

  public static CommandActor create(String worldName) {
    return new CommandActor(worldName);
  }

}
