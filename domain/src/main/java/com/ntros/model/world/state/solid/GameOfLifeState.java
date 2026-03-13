package com.ntros.model.world.state.solid;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.engine.gameoflife.fast.BitGrid;
import com.ntros.model.world.protocol.TileType;
import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.dimension.Dimension;
import com.ntros.model.world.state.dimension.Dimension2D;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameOfLifeState implements GridState {

  private final String worldName;
  private final Dimension dimension;

  private final BitGrid grid;
  private final BitGrid next;

  private final Map<String, Entity> entityMap = new LinkedHashMap<>();
  private final Map<Vector4, String> positionMap = new HashMap<>();
  private final Map<String, Vector4> moveIntentMap = new HashMap<>();

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
  public Map<String, Entity> entities() {
    return entityMap;
  }

  @Override
  public Map<Vector4, String> takenPositions() {
    return positionMap;
  }

  @Override
  public Map<String, Vector4> moveIntents() {
    return moveIntentMap;
  }

  @Override
  public Map<Vector4, TileType> terrain() {
    throw new UnsupportedOperationException("BitGrid world does not expose terrain map");
  }

  @Override
  public TileType getTileTypeAt(Vector4 pos) {
    return grid.get((int) pos.getX(), (int) pos.getY())
        ? TileType.ALIVE
        : TileType.EMPTY;
  }

  @Override
  public boolean isLegalMove(Vector4 vector4) {
    return isWithinBounds(vector4);
  }

  @Override
  public boolean isWithinBounds(Vector4 vec) {
    return vec.getX() >= 0 &&
        vec.getX() < dimension.getWidth() &&
        vec.getY() >= 0 &&
        vec.getY() < dimension.getHeight();
  }
}