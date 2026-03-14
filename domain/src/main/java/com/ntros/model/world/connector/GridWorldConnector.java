package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.engine.core.GridEngine;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.GridState;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base connector that binds a typed {@link GridState} to a typed {@link GridEngine}.
 *
 * <p>All shared, state-agnostic operations ({@link #update()}, {@link #snapshot()},
 * {@link #getWorldName()}, etc.) are implemented here.  Subclasses receive concrete type
 * parameters and implement {@link #apply(WorldOp)} without any {@code instanceof} checks —
 * the correct types are guaranteed by the constructor signature.
 *
 * <p>Two concrete subclasses cover the current world types:
 * <ul>
 *   <li>{@link PlayerWorldConnector} — arena / solo worlds ({@code PlayerGridState} +
 *       {@code PlayerGridEngine})</li>
 *   <li>{@link SimulationWorldConnector} — GOL / Falling-Sand worlds
 *       ({@code SimulationGridState} + {@code SimulationGridEngine})</li>
 * </ul>
 *
 * @param <S> the concrete grid-state type
 * @param <E> the concrete grid-engine type
 */
@Slf4j
public abstract class GridWorldConnector<S extends GridState, E extends GridEngine>
    implements WorldConnector {

  protected final S state;
  protected final E engine;
  protected final WorldCapabilities caps;

  protected GridWorldConnector(S state, E engine, WorldCapabilities caps) {
    this.state = state;
    this.engine = engine;
    this.caps = caps;
  }

  // ── Abstract ──────────────────────────────────────────────────────────────

  @Override
  public abstract WorldResult apply(WorldOp op);

  // ── Shared implementations ────────────────────────────────────────────────

  @Override
  public void update() {
    engine.applyIntents(state);
  }

  /** Returns a typed POJO snapshot suitable for JSON serialisation. */
  @Override
  public Object snapshot() {
    return engine.snapshot(state);
  }

  @Override
  public String snapshot(boolean oneLine) {
    return oneLine ? engine.serializeOneLine(state) : engine.serialize(state);
  }

  @Override
  public String getWorldName() {
    return state.worldName();
  }

  @Override
  public String getWorldType() {
    return state.worldType();
  }

  /** Returns the active player entities.  Overridden in {@link PlayerWorldConnector}. */
  @Override
  public List<Entity> getCurrentEntities() {
    return List.of();
  }

  @Override
  public WorldCapabilities getCapabilities() {
    return caps;
  }

  @Override
  public void reset() {
    engine.reset(state);
  }

  /**
   * Returns the underlying state for read-only access by the persistence layer
   * (e.g. to save terrain snapshots on shutdown).
   *
   * <p>The covariant return type in each subclass narrows this to the concrete state type,
   * so callers can access typed state members without casting.
   */
  public S getState() {
    return state;
  }
}
