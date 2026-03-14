package com.ntros.model.world.protocol.request;

/**
 * Subcommands the orchestrator client can issue to a Game-of-Life instance.
 */
public enum OrchestrateAction {
  /** Place a specific set of cells alive (additive — existing live cells are kept). */
  SEED,
  /** Fill the grid with a random population at a given density (0.0–1.0). */
  RANDOM_SEED,
  /** Toggle the live/dead state of specific cells. */
  TOGGLE,
  /** Kill every live cell — blank-slate the grid. */
  CLEAR,
  /** Place a single material cell at a specific position (Falling Sand). */
  PLACE,
  SET_WIND
}
