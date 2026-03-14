package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.connector.ops.MoveOp;
import com.ntros.model.world.connector.ops.OrchestrateOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.engine.core.PlayerGridEngine;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.PlayerGridState;
import java.util.List;

/**
 * Connector for player-driven worlds (arena, solo, etc.).
 *
 * <p>Holds a {@link PlayerGridState} and a {@link PlayerGridEngine} as concrete types, so
 * {@link #apply(WorldOp)} can delegate directly to the engine without any {@code instanceof}
 * checks.
 */
public class PlayerWorldConnector
    extends GridWorldConnector<PlayerGridState, PlayerGridEngine> {

  public PlayerWorldConnector(PlayerGridState state, PlayerGridEngine engine,
      WorldCapabilities caps) {
    super(state, engine, caps);
  }

  @Override
  public WorldResult apply(WorldOp op) {
    return switch (op) {
      case JoinOp j -> engine.joinEntity(j.req(), state);
      case MoveOp m -> engine.storeMoveIntent(m.req(), state);
      case RemoveOp r -> {
        engine.removeEntity(r.removeRequest().entityId(), state);
        yield WorldResult.succeeded(r.removeRequest().entityId(), state.worldName(), "ok");
      }
      case OrchestrateOp ignored ->
          WorldResult.failed("orchestrator", state.worldName(),
              "Player world does not support orchestration.");
      default -> throw new IllegalStateException("Unexpected op type: " + op);
    };
  }

  @Override
  public List<Entity> getCurrentEntities() {
    return state.entities().values().stream().toList();
  }
}
