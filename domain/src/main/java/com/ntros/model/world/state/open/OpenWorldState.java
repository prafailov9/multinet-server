package com.ntros.model.world.state.open;

import com.ntros.model.entity.movement.vectors.Vector3;
import com.ntros.model.entity.open.OpenWorldEntity;
import com.ntros.model.world.state.dimension.Dimension3D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Concrete, mutable state for a continuous 3D open world.
 *
 * <p>Constructor accepts explicit width/height/depth so the bounding volume matches the
 * {@link Dimension3D} used by the engine for boundary enforcement.
 */
public class OpenWorldState implements com.ntros.model.world.state.core.OpenWorldState {

  private final String     worldName;
  private final Dimension3D dimension;

  /** Entity registry — keyed by player name, preserving join order. */
  private final Map<String, OpenWorldEntity> entities;

  /**
   * Thrust-direction intents accumulated between ticks.
   * Keyed by entity name; values are the raw (possibly un-normalised) direction vectors
   * as received from the client. The engine normalises them during {@code applyIntents}.
   */
  private final Map<String, Vector3> movementIntents;

  public OpenWorldState(String worldName, int width, int height, int depth) {
    this.worldName  = worldName;
    this.dimension  = new Dimension3D(width, height, depth);
    this.entities        = new LinkedHashMap<>();
    this.movementIntents = new HashMap<>();
  }

  // ── DynamicWorldState ─────────────────────────────────────────────────────

  @Override
  public String worldName() {
    return worldName;
  }

  @Override
  public String worldType() {
    return "OPEN";
  }

  @Override
  public Dimension3D dimension() {
    return dimension;
  }

  @Override
  public Map<String, OpenWorldEntity> entities() {
    return entities;
  }

  @Override
  public Map<String, Vector3> moveIntents() {
    return movementIntents;
  }
}
