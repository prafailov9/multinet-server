package com.ntros.model.world.protocol.request;

import com.ntros.model.entity.movement.grid.Position;
import java.util.List;

/**
 * Carries an orchestrator command for a Game-of-Life (or similar) world.
 *
 * <p>Field usage by action:
 * <ul>
 *   <li>{@link OrchestrateAction#SEED}        — {@code cells} lists the positions to make alive.</li>
 *   <li>{@link OrchestrateAction#RANDOM_SEED} — {@code density} (0.0–1.0) controls live-cell probability.</li>
 *   <li>{@link OrchestrateAction#TOGGLE}      — {@code cells} lists positions to flip.</li>
 *   <li>{@link OrchestrateAction#CLEAR}       — no additional fields needed.</li>
 * </ul>
 */
public record OrchestrateRequest(
    OrchestrateAction action,
    List<Position> cells,
    float density
) implements ClientRequest {

  /** Convenience factory for CLEAR (no positional data). */
  public static OrchestrateRequest clear() {
    return new OrchestrateRequest(OrchestrateAction.CLEAR, List.of(), 0f);
  }

  /** Convenience factory for RANDOM_SEED. */
  public static OrchestrateRequest randomSeed(float density) {
    return new OrchestrateRequest(OrchestrateAction.RANDOM_SEED, List.of(), density);
  }
}
