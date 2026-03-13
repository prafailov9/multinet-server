package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.connector.ops.OrchestrateOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.wator.WaTorEngine;
import com.ntros.model.world.wator.WaTorWorld;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Facade connecting the server infrastructure (actor, session layer) to the Wa-Tor
 * ECS simulation.
 *
 * <p>Accepted {@link WorldOp} types:
 * <ul>
 *   <li>{@link JoinOp}        — register an observer session</li>
 *   <li>{@link RemoveOp}      — deregister an observer session</li>
 *   <li>{@link OrchestrateOp} — future: configure sim parameters at runtime</li>
 * </ul>
 *
 * <p>The {@link #update()} method is called by the clock on every tick and advances
 * the ECS simulation by a fixed physics step.
 *
 * <p>Wa-Tor entities are autonomous — there is no player MOVE command.
 * Clients join to observe and optionally ORCHESTRATE (e.g. adjust density or pause).
 */
@Slf4j
public class WaTorConnector implements WorldConnector {

  /** Fixed physics step: 120 Hz → ~8.33 ms per tick. */
  private static final float TICK_DT_SECONDS = 1f / 120f;

  private final WaTorWorld        world;
  private final WaTorEngine       engine;
  private final WorldCapabilities caps;

  public WaTorConnector(WaTorWorld world, WaTorEngine engine, WorldCapabilities caps) {
    this.world  = world;
    this.engine = engine;
    this.caps   = caps;
  }

  // ── WorldConnector ────────────────────────────────────────────────────────

  @Override
  public WorldResult apply(WorldOp op) {
    return switch (op) {
      case JoinOp    j -> engine.joinObserver(j.req(), world);
      case RemoveOp  r -> {
        engine.removeObserver(r.removeRequest().entityId(), world);
        yield WorldResult.succeeded(r.removeRequest().entityId(),
            world.getState().worldName(), "Observer removed.");
      }
      case OrchestrateOp o -> {
        // Placeholder: future ORCHESTRATE sub-commands (pause, reset, set-density, etc.)
        log.info("[WaTorConnector] ORCHESTRATE received: {}", o.req().action());
        yield WorldResult.succeeded("orchestrator", world.getState().worldName(),
            "Orchestrate acknowledged.");
      }
      default -> throw new IllegalStateException(
          "[WaTorConnector] Unsupported op: " + op.getClass().getSimpleName());
    };
  }

  @Override
  public void update() {
    engine.tick(world, TICK_DT_SECONDS);
  }

  @Override
  public Object snapshot() {
    return engine.snapshot(world);
  }

  /** Not used for Wa-Tor — snapshot() returns typed POJOs. */
  @Override
  public String snapshot(boolean oneLine) {
    return "";
  }

  @Override
  public String getWorldName() {
    return world.getState().worldName();
  }

  @Override
  public String getWorldType() {
    return "WATOR";
  }

  /**
   * Wa-Tor has no grid-entity concept visible to the server layer.
   * Returns an empty list — observer sessions are tracked inside the engine.
   */
  @Override
  public List<Entity> getCurrentEntities() {
    return Collections.emptyList();
  }

  @Override
  public WorldCapabilities getCapabilities() {
    return caps;
  }

  @Override
  public void reset() {
    engine.reset(world);
  }
}
