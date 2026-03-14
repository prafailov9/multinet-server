package com.ntros.model.world.connector;

import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.connector.ops.MoveOp;
import com.ntros.model.world.connector.ops.OrchestrateOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.engine.core.SimulationGridEngine;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.SimulationGridState;

/**
 * Connector for orchestration-driven simulation worlds (Game of Life, Falling Sand, etc.).
 *
 * <p>Holds a {@link SimulationGridState} and a {@link SimulationGridEngine} as concrete types,
 * so {@link #apply(WorldOp)} can delegate directly to the engine without any {@code instanceof}
 * checks.  Player operations (JOIN, MOVE) are rejected at the connector level since simulation
 * worlds do not have player entities.
 */
public class SimulationWorldConnector
    extends GridWorldConnector<SimulationGridState, SimulationGridEngine> {

  public SimulationWorldConnector(SimulationGridState state, SimulationGridEngine engine,
      WorldCapabilities caps) {
    super(state, engine, caps);
  }

  /**
   * Seeds the world state directly, <em>without</em> starting the simulation clock.
   *
   * <p>Use only during server bootstrap to pre-populate the initial world state
   * (e.g. a random sand distribution).  Because this bypasses the instance lifecycle,
   * the clock remains stopped and the world will only begin ticking when the first
   * client sends an ORCHESTRATE command — preserving the {@code ORCHESTRATION_DRIVEN}
   * contract.
   *
   * <p><b>Thread safety:</b> safe to call synchronously on the main thread during startup
   * before any client connections have been accepted.
   *
   * @param req the seed request to apply (e.g. {@link OrchestrateRequest#randomSeed(float)})
   * @return the result from the engine
   */
  public WorldResult preSeed(OrchestrateRequest req) {
    return engine.orchestrate(req, state);
  }

  @Override
  public WorldResult apply(WorldOp op) {
    return switch (op) {
      case OrchestrateOp o -> engine.orchestrate(o.req(), state);
      case JoinOp j ->
          WorldResult.succeeded(j.req().playerName(), state.worldName(), "observer");
      case MoveOp m ->
          WorldResult.failed(m.req().playerId(), state.worldName(),
              "Simulation world does not support player movement.");
      case RemoveOp r ->
          // No player entities — treat as no-op and acknowledge cleanly.
          WorldResult.succeeded(r.removeRequest().entityId(), state.worldName(), "ok");
      default -> throw new IllegalStateException("Unexpected op type: " + op);
    };
  }
}
