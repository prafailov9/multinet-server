package com.ntros.model.entity.config.access;

/**
 * Runtime configuration for a {@code ServerInstance}.
 *
 * <p>This record holds only <em>tunable</em> per-instance values — things an operator can
 * adjust at deploy time without changing the world type. Properties that are intrinsic to a
 * world type (whether it supports players, its lifecycle policy, determinism, seed) live in
 * {@code WorldCapabilities}.
 */
public record InstanceSettings(
    /** Maximum number of simultaneously active player sessions. */
    int maxPlayers,

    /** Who may join this instance. */
    InstanceVisibility instanceVisibility,

    /** State broadcast rate sent to clients (frames per second). */
    int broadcastHz,

    /** Simulation tick rate while the world is active (ticks per second). */
    int activeTps,

    /** Reduced tick rate after {@link #idleAfterSeconds} of no player activity. */
    int idleTps,

    /** Seconds of inactivity before the instance drops to {@link #idleTps}. */
    int idleAfterSeconds
) {

  // ── Convenience factories ───────────────────────────────────────────────────

  public static InstanceSettings multiplayerDefault() {
    return new InstanceSettings(100, InstanceVisibility.PUBLIC, 32, 120, 10, 30);
  }

  public static InstanceSettings multiplayer(int broadcastHz) {
    return new InstanceSettings(100, InstanceVisibility.PUBLIC, broadcastHz, 120, 10, 30);
  }

  public static InstanceSettings multiplayerJoinable() {
    return new InstanceSettings(100, InstanceVisibility.JOINABLE, 20, 120, 10, 30);
  }

  public static InstanceSettings singlePlayerDefault() {
    return new InstanceSettings(1, InstanceVisibility.PRIVATE, 20, 120, 10, 30);
  }

  /**
   * Configuration for simulation worlds (Game of Life, Falling Sand, etc.).
   * Visibility PUBLIC; observers may join to watch.
   */
  public static InstanceSettings simulation(int broadcastHz) {
    return new InstanceSettings(100, InstanceVisibility.PUBLIC, broadcastHz, 120, 10, 30);
  }

  public static InstanceSettings simulationDefault() {
    return new InstanceSettings(100, InstanceVisibility.PUBLIC, 20, 120, 10, 30);
  }
}
