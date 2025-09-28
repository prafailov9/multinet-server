package com.ntros.model.world.state;

import com.ntros.model.world.state.solid.GridWorldState;

public class WorldStateFactory {

  public static GridWorldState createGridWorld(String worldName, int width, int height) {
    return new GridWorldState(worldName, width, height);
  }

  public static GridState createWorldState(String type, String worldName, int width, int height) {
    return switch (type) {
      case "GRID" -> new GridWorldState(worldName, width, height);
      default -> throw new RuntimeException("Type world does not exist.");
    };
  }

}
