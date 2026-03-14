package com.ntros.model.world.state.grid;

import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.engine.d2.grid.gameoflife.fast.BitGrid;
import com.ntros.model.world.state.core.SimulationGridState;
import com.ntros.model.world.state.dimension.Dimension;
import com.ntros.model.world.state.dimension.Dimension2D;

public class GameOfLifeState implements SimulationGridState {

  private final String worldName;
  private final Dimension dimension;

  // BitGrid members available for introspection; the engine owns the fast long[] working buffers.
  private final BitGrid grid;
  private final BitGrid next;

  public GameOfLifeState(String worldName, int width, int height) {
    this.worldName = worldName;
    this.dimension = new Dimension2D(width, height);
    this.grid = new BitGrid(width, height);
    this.next = new BitGrid(width, height);
  }

  public BitGrid grid() {
    return grid;
  }

  public BitGrid next() {
    return next;
  }

  @Override
  public String worldName() {
    return worldName;
  }

  @Override
  public String worldType() {
    return "GAME_OF_LIFE";
  }

  @Override
  public Dimension dimension() {
    return dimension;
  }

  @Override
  public boolean isWithinBounds(Vector4 vec) {
    return vec.getX() >= 0 &&
        vec.getX() < dimension.getWidth() &&
        vec.getY() >= 0 &&
        vec.getY() < dimension.getHeight();
  }
}
