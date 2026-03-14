package com.ntros.model.world.state.d2.grid;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.dimension.Dimension;
import com.ntros.model.world.state.dimension.Dimension2D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * World state for the Falling Sand simulation.
 *
 * <p>Terrain is intentionally NOT exposed through the {@link GridState} interface — the
 * {@link com.ntros.model.world.engine.d2.grid.fallingsand.FallingSandEngine} owns the live
 * cell buffer as a flat {@code CellType[]} for performance. Calling {@link #terrain()} or
 * {@link #getCellTypeAt(Vector4)} throws {@link UnsupportedOperationException}; callers that
 * need cell data must go through the engine's buffer directly.
 *
 * <p>Clients connect as <em>observers</em>: they are registered in {@link #entities()} for
 * lifecycle tracking (join/leave) but never occupy a tile, so {@link #takenPositions()} is
 * always empty and {@link #isLegalMove(Vector4)} always returns {@code false}.
 */
public class FallingSandState implements GridState {

  private final String    worldName;
  private final Dimension dimension;

  private final Map<String, Entity>  entityMap     = new LinkedHashMap<>();
  private final Map<Vector4, String> positionMap   = new HashMap<>();
  private final Map<String, Vector4> moveIntentMap = new HashMap<>();

  public FallingSandState(String worldName, int width, int height) {
    this.worldName = worldName;
    this.dimension = new Dimension2D(width, height);
  }

  @Override public Dimension dimension()  { return dimension; }
  @Override public String    worldName()  { return worldName; }
  @Override public String    worldType()  { return "FALLING_SAND"; }

  @Override public Map<String, Entity>  entities()       { return entityMap; }
  @Override public Map<Vector4, String> takenPositions() { return positionMap; }
  @Override public Map<String, Vector4> moveIntents()    { return moveIntentMap; }

  /**
   * Not supported — the engine owns the flat cell buffer.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public Map<Vector4, CellType> terrain() {
    throw new UnsupportedOperationException(
        "FallingSandState does not expose a terrain map. "
            + "Use FallingSandEngine's internal buffer instead.");
  }

  /**
   * Not supported — terrain lookup goes through the engine's internal buffer.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public CellType getCellTypeAt(Vector4 pos) {
    throw new UnsupportedOperationException(
        "FallingSandState does not support getCellTypeAt. "
            + "Use FallingSandEngine's internal buffer instead.");
  }

  /** Observer sessions never move; there is no grid entity to block. */
  @Override
  public boolean isLegalMove(Vector4 position) {
    return false;
  }

  @Override
  public boolean isWithinBounds(Vector4 position) {
    int x = (int) position.getX();
    int y = (int) position.getY();
    return x >= 0 && x < dimension.getWidth()
        && y >= 0 && y < dimension.getHeight();
  }
}
