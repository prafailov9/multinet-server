package com.ntros.model.world.protocol.request;

import com.ntros.model.entity.movement.grid.Position;
import java.util.List;

/**
 * Carries an orchestrator command for a grid world.
 *
 * <p>Field usage by action:
 * <ul>
 *   <li>{@link OrchestrateAction#SEED}        — {@code cells} lists the positions to make alive.</li>
 *   <li>{@link OrchestrateAction#RANDOM_SEED} — {@code density} (0.0–1.0) controls live-cell probability.</li>
 *   <li>{@link OrchestrateAction#TOGGLE}      — {@code cells} lists positions to flip.</li>
 *   <li>{@link OrchestrateAction#CLEAR}       — no additional fields needed.</li>
 *   <li>{@link OrchestrateAction#PLACE}       — {@code cells} has one position; {@code material} names the CellType.</li>
 * </ul>
 */
public record OrchestrateRequest(
    OrchestrateAction action,
    List<Position> cells,
    float density,
    String material
) implements ClientRequest {

  /** Convenience factory for CLEAR (no positional data). */
  public static OrchestrateRequest clear() {
    return new OrchestrateRequest(OrchestrateAction.CLEAR, List.of(), 0f, null);
  }

  /** Convenience factory for RANDOM_SEED. */
  public static OrchestrateRequest randomSeed(float density) {
    return new OrchestrateRequest(OrchestrateAction.RANDOM_SEED, List.of(), density, null);
  }

  /** Convenience factory for PLACE (Falling Sand, Wildfire). */
  public static OrchestrateRequest place(String material, int x, int y) {
    return new OrchestrateRequest(OrchestrateAction.PLACE, List.of(Position.of(x, y)), 0f, material);
  }

  /** Convenience factory for SET_WIND (Wildfire). direction e.g. "N", "SE". speed 0.0–1.0. */
  public static OrchestrateRequest setWind(String direction, float speed) {
    return new OrchestrateRequest(OrchestrateAction.SET_WIND, List.of(), speed, direction);
  }

  /** Convenience factory for SEED (multi-cell tree placement, Wildfire). */
  public static OrchestrateRequest seed(List<Position> cells) {
    return new OrchestrateRequest(OrchestrateAction.SEED, List.copyOf(cells), 0f, null);
  }
}
