package com.ntros.model.entity.config.access;

/**
 * Settings for configuring an ServerInstance
 */
public record InstanceSettings(
    int maxPlayers,
    boolean requiresOrchestrator,
    InstanceVisibility instanceVisibility,
    boolean autoStartOnPlayerJoin,
    int broadcastHz,
    int activeTps,
    int idleTps,
    int idleAfterSeconds,
    // TODO: these are not part of instance cfg. Move to WorldSettings
    boolean deterministic,
    Long seed
) {

  public static InstanceSettings multiplayer(int broadcastHz) {
    return new InstanceSettings(
        100, false, InstanceVisibility.PUBLIC, true,
        broadcastHz,
        120, 10, 30,
        false, null
    );
  }

  public static InstanceSettings multiplayerDefault() {
    return new InstanceSettings(
        100, false, InstanceVisibility.PUBLIC, true,
        20,
        120, 10, 30,
        false, null
    );
  }

  public static InstanceSettings multiplayerJoinable() {
    return new InstanceSettings(
        100, false, InstanceVisibility.JOINABLE, true,
        20,          // broadcastHz
        120, 10, 30, // activeTps, idleTps, idleAfterSeconds
        false, null  // deterministic, seed
    );
  }

  public static InstanceSettings singlePlayerDefault() {
    return new InstanceSettings(
        1, false, InstanceVisibility.PRIVATE, false,
        20,          // broadcastHz
        120, 10, 30, // activeTps, idleTps, idleAfterSeconds
        false, null  // deterministic, seed
    );
  }

  public static InstanceSettings singlePlayerOrchestrator() {
    return new InstanceSettings(
        1, true, InstanceVisibility.PRIVATE, false,
        20,          // broadcastHz
        120, 10, 30, // activeTps, idleTps, idleAfterSeconds
        false, null  // deterministic, seed
    );
  }

  /**
   * Multiplayer world that requires an orchestrator to seed / control it (e.g. Game-of-Life).
   * Players may join to observe, but the clock only starts when an ORCHESTRATE command arrives.
   */
  public static InstanceSettings multiplayerOrchestrator(int broadcastHz) {
    return new InstanceSettings(
        100, true, InstanceVisibility.PUBLIC, true,
        broadcastHz,
        120, 10, 30,
        false, null
    );
  }

  /**
   * Autonomous simulation world (e.g. Wa-Tor predator-prey).
   * Observers may join to watch, but the simulation runs continuously from server boot —
   * it does not start/stop based on player presence.
   * {@code autoStartOnPlayerJoin = false}: lifecycle is managed externally by the bootstrap.
   */
  public static InstanceSettings autonomousSimulation(int broadcastHz) {
    return new InstanceSettings(
        100, false, InstanceVisibility.PUBLIC, false,
        broadcastHz,
        120, 10, 30,
        false, null
    );
  }

}