package com.ntros.model.world.state.d2.grid;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.engine.d2.grid.gameoflife.fast.BitGrid;
import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.dimension.Dimension;
import com.ntros.model.world.state.dimension.Dimension2D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FallingSandState implements GridState {

  private final String worldName;
  private final Dimension dimension;

  private final Map<String, Entity> entityMap = new LinkedHashMap<>();
  private final Map<Vector4, String> positionMap = new HashMap<>();
  private final Map<String, Vector4> moveIntentMap = new HashMap<>();

  public FallingSandState(String worldName, int width, int height) {
    this.worldName = worldName;
    this.dimension = new Dimension2D(width, height);
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
    return takenPositions();
  }

  @Override
  public Map<String, Vector4> moveIntents() {
    return moveIntentMap;
  }

  @Override
  public Map<Vector4, CellType> terrain() {
    throw new UnsupportedOperationException("FallingSand World world does not expose terrain map");
  }

  @Override
  public CellType getCellTypeAt(Vector4 pos) {
    return null;
  }

  @Override
  public boolean isLegalMove(Vector4 position) {
    return false;
  }

  @Override
  public boolean isWithinBounds(Vector4 position) {
    return false;
  }

  @Override
  public String worldName() {
    return "";
  }

  @Override
  public String worldType() {
    return "";
  }
}
