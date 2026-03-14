package com.ntros.model.entity.config;

import com.ntros.model.entity.config.access.InstanceRole;

/**
 * Static descriptor of what a world type <em>is</em> and what it can do.
 *
 * <p>These values are determined at world-creation time and never change at runtime.
 * Mutable per-instance settings (max players, tick rate, visibility) live in
 * {@code InstanceSettings}.
 *
 * <h3>Lifecycle</h3>
 * {@link LifecyclePolicy} replaces the old {@code requiresOrchestrator} and
 * {@code autoStartOnPlayerJoin} booleans with an explicit policy enum.
 *
 * <h3>Determinism</h3>
 * {@code deterministic} and {@code seed} have moved here from {@code InstanceSettings} because
 * they are intrinsic to the simulation algorithm, not to a runtime configuration.
 *
 * <h3>Orchestration access</h3>
 * {@code minimumOrchestratorRole} declares the lowest {@link InstanceRole} permitted to send
 * ORCHESTRATE commands. The {@code InstanceAccessController} reads this field so no hard-coded
 * role checks are needed in individual command handlers.
 */
public record WorldCapabilities(
    /** Whether clients can join as active participants (entities exist in the world). */
    boolean supportsPlayers,

    /** Whether ORCHESTRATE commands are accepted by this world type. */
    boolean supportsOrchestrator,

    /** Whether the world contains server-controlled AI / NPC entities. */
    boolean hasAIEntities,

    /** Whether the simulation produces the same output given the same seed. */
    boolean deterministic,

    /** Optional fixed seed for deterministic worlds; {@code null} means random each run. */
    Long seed,

    /** When the simulation clock starts and stops for this world type. */
    LifecyclePolicy lifecyclePolicy,

    /**
     * The minimum {@link InstanceRole} a client must hold to send ORCHESTRATE commands.
     * Ignored (but still required) when {@link #supportsOrchestrator()} is {@code false}.
     */
    InstanceRole minimumOrchestratorRole
) {

  // ── Convenience factories ───────────────────────────────────────────────────

  /**
   * Standard arena: player-driven clock, no orchestration, non-deterministic.
   */
  public static WorldCapabilities arena() {
    return new WorldCapabilities(
        true, false, false,
        false, null,
        LifecyclePolicy.PLAYER_DRIVEN,
        InstanceRole.GAME_MASTER   // unused — supportsOrchestrator=false
    );
  }

  /**
   * Game-of-Life: orchestration-driven clock, deterministic, no player entities.
   * Any PLAYER or above may send ORCHESTRATE.
   */
  public static WorldCapabilities gameOfLife() {
    return new WorldCapabilities(
        false, true, false,
        true, null,
        LifecyclePolicy.ORCHESTRATION_DRIVEN,
        InstanceRole.PLAYER
    );
  }

  /**
   * Falling Sand: orchestration-driven clock, non-deterministic, no player entities.
   * Any PLAYER or above may send ORCHESTRATE.
   */
  public static WorldCapabilities fallingSand() {
    return new WorldCapabilities(
        false, true, false,
        false, null,
        LifecyclePolicy.ORCHESTRATION_DRIVEN,
        InstanceRole.PLAYER
    );
  }

  /**
   * Wa-Tor: autonomous AI-driven predator/prey simulation, no player entities, no orchestration.
   * The clock runs from server bootstrap; observer sessions may join to watch.
   */
  public static WorldCapabilities waTor() {
    return new WorldCapabilities(
        false, false, true,
        false, null,
        LifecyclePolicy.AUTONOMOUS,
        InstanceRole.GAME_MASTER   // unused — supportsOrchestrator=false
    );
  }

  /**
   * Boids: autonomous ECS-backed flocking simulation, no player entities, no orchestration.
   * The clock runs from server bootstrap; observer sessions may join to watch.
   */
  public static WorldCapabilities boids() {
    return new WorldCapabilities(
        false, false, true,
        false, null,
        LifecyclePolicy.AUTONOMOUS,
        InstanceRole.GAME_MASTER   // unused — supportsOrchestrator=false
    );
  }

  public static WorldCapabilities wildfire() {
    return new WorldCapabilities(
        false, true, false,
        false, null,
        LifecyclePolicy.ORCHESTRATION_DRIVEN,
        InstanceRole.PLAYER
    );
  }

}
