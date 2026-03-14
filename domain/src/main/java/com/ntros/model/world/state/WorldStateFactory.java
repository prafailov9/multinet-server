package com.ntros.model.world.state;

import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.grid.ArenaGridState;
import com.ntros.model.world.state.open.OpenWorldState;

public class WorldStateFactory {

  public static ArenaGridState createGridWorld(String worldName, int width, int height) {
    return new ArenaGridState(worldName, width, height);
  }

  public static OpenWorldState createOpenWorld(String worldName, int width, int height, int depth) {
    return new OpenWorldState(worldName, width, height, depth);
  }

  public static GridState createWorldState(String type, String worldName, int width, int height) {
    return switch (type) {
      case "GRID" -> new ArenaGridState(worldName, width, height);
      default -> throw new RuntimeException("Type world does not exist.");
    };
  }

}
