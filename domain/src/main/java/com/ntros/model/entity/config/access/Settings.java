package com.ntros.model.entity.config.access;

/**
 * Settings for configuring an ServerInstance
 *
 * @param maxPlayers
 * @param requiresOrchestrator
 * @param visibility
 * @param autoStartOnPlayerJoin
 * @param broadcastHz
 * @param activeTps
 * @param idleTps
 * @param idleAfterSeconds
 * @param deterministic
 * @param seed
 */
public record Settings(
    int maxPlayers,
    boolean requiresOrchestrator,
    Visibility visibility,
    boolean autoStartOnPlayerJoin,
    int broadcastHz,
    int activeTps,
    int idleTps,
    int idleAfterSeconds,
    boolean deterministic,
    Long seed,
    boolean stageMoves
) {

  public static Settings multiplayer(int broadcastHz) {
    return new Settings(
        100, false, Visibility.PUBLIC, true,
        broadcastHz,
        120, 10, 30,
        false, null, true
    );
  }

  public static Settings multiplayerDefault() {
    return new Settings(
        100, false, Visibility.PUBLIC, true,
        20,
        120, 10, 30,
        false, null, false
    );
  }

  public static Settings multiplayerJoinable() {
    return new Settings(
        100, false, Visibility.JOINABLE, true,
        20,          // broadcastHz
        120, 10, 30, // activeTps, idleTps, idleAfterSeconds
        false, null  // deterministic, seed
        , true
    );
  }

  public static Settings singlePlayerDefault() {
    return new Settings(
        1, false, Visibility.PRIVATE, false,
        20,          // broadcastHz
        120, 10, 30, // activeTps, idleTps, idleAfterSeconds
        false, null  // deterministic, seed
        , true
    );
  }

  public static Settings singlePlayerOrchestrator() {
    return new Settings(
        1, true, Visibility.PRIVATE, false,
        20,          // broadcastHz
        120, 10, 30, // activeTps, idleTps, idleAfterSeconds
        false, null  // deterministic, seed
        , true
    );
  }

}