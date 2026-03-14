package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.entity.movement.grid.Position;
import com.ntros.model.entity.open.OpenWorldEntity;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.connector.ops.OpenMoveOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.engine.d3.open.OpenWorldEngine;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.open.OpenWorldSnapshot;
import com.ntros.model.world.state.open.OpenWorldSnapshot.BoundsView;
import com.ntros.model.world.state.open.OpenWorldSnapshot.EntityView3D;
import com.ntros.model.world.state.dimension.Dimension3D;
import com.ntros.model.world.state.open.OpenWorldState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Connector between the server infrastructure (actor, session layer) and the 3D open world engine.
 *
 * <p>Accepted {@link WorldOp} types for open world:
 * <ul>
 *   <li>{@link JoinOp} — spawn a new player entity</li>
 *   <li>{@link OpenMoveOp} — stage a free-movement thrust intent</li>
 *   <li>{@link RemoveOp} — despawn an entity</li>
 * </ul>
 *
 * <p>The {@link #update()} method is called by the clock on every tick and delegates to
 * {@link OpenWorldEngine#applyIntents} with a fixed delta-time matching the server tick interval.
 * For production the delta-time should be derived from the actual elapsed time, but using a
 * constant avoids non-determinism in tests.
 */
@Slf4j
public class OpenWorldConnector implements WorldConnector {

  /** Fixed physics step duration (seconds) — 20 ms ≈ 50 TPS. */
  private static final float TICK_DT_SECONDS = 0.020f;

  private final OpenWorldState    state;
  private final OpenWorldEngine   engine;
  private final WorldCapabilities caps;

  public OpenWorldConnector(OpenWorldState state, OpenWorldEngine engine, WorldCapabilities caps) {
    this.state  = state;
    this.engine = engine;
    this.caps   = caps;
  }

  // ── WorldConnector ────────────────────────────────────────────────────────

  @Override
  public WorldResult apply(WorldOp op) {
    return switch (op) {
      case JoinOp     j -> engine.joinEntity(j.req(), state);
      case OpenMoveOp m -> engine.storeMoveIntent(m.req(), state);
      case RemoveOp   r -> {
        engine.removeEntity(r.removeRequest().entityId(), state);
        yield WorldResult.succeeded(r.removeRequest().entityId(), state.worldName(), "ok");
      }
      default -> throw new IllegalStateException(
          "[OpenWorldConnector] Unexpected op: " + op.getClass().getSimpleName());
    };
  }

  @Override
  public void update() {
    engine.applyIntents(state, TICK_DT_SECONDS);
  }

  // ── Snapshot ──────────────────────────────────────────────────────────────

  /**
   * Returns a typed {@link OpenWorldSnapshot} POJO (serialised to JSON by the broadcast layer).
   */
  @Override
  public Object snapshot() {
    Dimension3D dim = state.dimension();
    BoundsView bounds = new BoundsView(dim.getWidth(), dim.getHeight(), dim.getDepth());

    Map<String, EntityView3D> ents = new LinkedHashMap<>();
    state.entities().forEach((name, entity) -> {
      var pos = entity.getPosition();
      var vel = entity.getVelocity();
      ents.put(name, new EntityView3D(
          pos.getX(), pos.getY(), pos.getZ(),
          vel.getDx(), vel.getDy(), vel.getDz(),
          entity.yaw(), entity.pitch()
      ));
    });

    return new OpenWorldSnapshot(bounds, ents);
  }

  /** Returns the one-line JSON string produced by the engine serialiser. */
  @Override
  public String snapshot(boolean oneLine) {
    return oneLine ? engine.serializeOneLine(state) : engine.serialize(state);
  }

  // ── Metadata ──────────────────────────────────────────────────────────────

  @Override
  public String getWorldName() {
    return state.worldName();
  }

  @Override
  public String getWorldType() {
    return state.worldType();
  }

  @Override
  public List<Entity> getCurrentEntities() {
    // OpenWorldEntity is not a grid Entity — return an adapted view for callers that only
    // need getName(). The list is read-only and not cached.
    return state.entities().values().stream()
        .map(OpenWorldEntityAdapter::new)
        .map(e -> (Entity) e)
        .toList();
  }

  @Override
  public WorldCapabilities getCapabilities() {
    return caps;
  }

  @Override
  public void reset() {
    engine.reset(state);
  }

  // ── Inner adapter ─────────────────────────────────────────────────────────

  /**
   * Thin adapter that bridges {@link OpenWorldEntity} to the grid-centric {@link Entity}
   * interface, exposing just the name. Position methods throw
   * {@link UnsupportedOperationException} because open-world positions are float-based and
   * cannot be represented as a discrete {@link Position}.
   */
  private record OpenWorldEntityAdapter(OpenWorldEntity delegate) implements Entity {

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public Position getPosition() {
      throw new UnsupportedOperationException(
          "Open-world entities have float positions — use OpenWorldConnector.snapshot() instead.");
    }

    @Override
    public void setPosition(Position position) {
      throw new UnsupportedOperationException(
          "Open-world entities use Vector3D positions.");
    }
  }
}
