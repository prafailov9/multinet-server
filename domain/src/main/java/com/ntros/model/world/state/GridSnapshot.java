package com.ntros.model.world.state;

import java.util.Map;

public record GridSnapshot(
    Map<String, String> tiles,              // "x,y" -> "EMPTY|WALL|TRAP|WATER"
    Map<String, EntityView> entities        // "name" -> {x,y}
) {

  public record EntityView(int x, int y) {

  }
}