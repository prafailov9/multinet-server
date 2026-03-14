package com.ntros.model.world.state.grid;

import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.state.core.SimulationGridState;
import com.ntros.model.world.state.dimension.Dimension;
import com.ntros.model.world.state.dimension.Dimension2D;

public class WildfireState implements SimulationGridState {

  private final String worldName;
  private final Dimension dimension;

  public WildfireState(String worldName, int width, int height) {
    this.worldName = worldName;
    this.dimension = new Dimension2D(width, height);
  }

  @Override
  public Dimension dimension() {
    return dimension;
  }

  @Override
  public String worldName() {
    return worldName;
  }

  @Override
  public String worldType() {
    return "WILDFIRE";
  }

  @Override
  public boolean isWithinBounds(Vector4 position) {
    int x = (int) position.getX();
    int y = (int) position.getY();
    return x >= 0 && x < dimension.getWidth() && y >= 0 && y < dimension.getHeight();
  }
}
