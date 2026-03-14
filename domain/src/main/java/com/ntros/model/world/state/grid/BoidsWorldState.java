package com.ntros.model.world.state.grid;

import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.state.core.SimulationGridState;
import com.ntros.model.world.state.dimension.Dimension;
import com.ntros.model.world.state.dimension.Dimension2D;

/**
 * Minimal state container for a boids flocking world.
 *
 * <p>Unlike grid-based simulations (GoL, Falling Sand) where the engine reads and writes
 * terrain through the state object, the boids engine owns all entity data internally inside
 * its {@link com.ntros.ecs.core.EcsWorld}. This state class therefore carries only the
 * immutable world metadata (name, dimensions) used by the connector and instance layers.
 */
public class BoidsWorldState implements SimulationGridState {

  private final String worldName;
  private final Dimension dimension;

  public BoidsWorldState(String worldName, int width, int height) {
    this.worldName = worldName;
    this.dimension = new Dimension2D(width, height);
  }

  @Override
  public String worldName() {
    return worldName;
  }

  @Override
  public String worldType() {
    return "BOIDS";
  }

  @Override
  public Dimension dimension() {
    return dimension;
  }

  @Override
  public boolean isWithinBounds(Vector4 position) {
    int x = (int) position.getX();
    int y = (int) position.getY();
    return x >= 0 && x < dimension.getWidth() && y >= 0 && y < dimension.getHeight();
  }
}
