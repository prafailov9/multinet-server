package com.ntros.lifecycle.instance.actor;

public final class Actors {

  public static WorldActor create(String worldName) {
    return new WorldActor(worldName);
  }

}
