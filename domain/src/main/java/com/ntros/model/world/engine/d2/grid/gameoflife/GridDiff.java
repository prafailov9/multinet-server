package com.ntros.model.world.engine.d2.grid.gameoflife;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.state.EntityView;
import java.util.List;
import java.util.Map;

/**
 * Represents the per-tick cell changes between the previous broadcast state and the current
 * generation, together with the current entity map.
 *
 * <p>Serialized by {@link GridDiffSerializer} as:
 * <pre>
 * {"type":"diff","born":[[x,y],…],"died":[[x,y],…],"entities":{"name":{"x":N,"y":N}}}
 * </pre>
 *
 * <p>The client applies born/died deltas to its local tile map, keeping it in sync with the
 * server without re-transmitting the full terrain on every broadcast tick.
 */
@JsonSerialize(using = GridDiffSerializer.class)
public class GridDiff {

  private final List<Vector4> bornCells;
  private final List<Vector4> diedCells;
  private final Map<String, EntityView> entities;

  public GridDiff(List<Vector4> bornCells, List<Vector4> diedCells,
      Map<String, EntityView> entities) {
    this.bornCells = bornCells;
    this.diedCells = diedCells;
    this.entities = entities;
  }

  public List<Vector4> getBornCells() {
    return bornCells;
  }

  public List<Vector4> getDiedCells() {
    return diedCells;
  }

  public Map<String, EntityView> getEntities() {
    return entities;
  }
}
