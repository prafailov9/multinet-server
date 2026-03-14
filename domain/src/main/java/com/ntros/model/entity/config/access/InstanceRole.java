package com.ntros.model.entity.config.access;

/**
 * Privilege level a client holds within a specific world instance.
 *
 * <p>Constants are declared in <em>ascending</em> privilege order so that ordinal comparisons
 * (via {@link #atLeast}) are meaningful:
 *
 * <pre>
 *  OBSERVER &lt; PLAYER &lt; MODERATOR &lt; ADMIN &lt; GAME_MASTER
 * </pre>
 *
 * <p>{@code GAME_MASTER} is the highest instance-level role. There is no separate OWNER role —
 * world ownership is represented by holding {@code GAME_MASTER} in that world.
 */
public enum InstanceRole {
  OBSERVER,     // read-only viewer; can watch but not interact
  PLAYER,       // standard participant; can move and interact
  MODERATOR,    // can kick/mute players
  ADMIN,        // can change world settings and manage moderators
  GAME_MASTER;  // unrestricted instance authority (top of the hierarchy)

  /**
   * Returns {@code true} when this role's privilege is at least as high as {@code minimum}.
   *
   * <p>Relies on enum ordinal (declaration order = ascending privilege), so the ordering of
   * constants above must not be changed without updating callers.
   *
   * <pre>
   *   PLAYER.atLeast(OBSERVER)    == true
   *   PLAYER.atLeast(PLAYER)      == true
   *   PLAYER.atLeast(MODERATOR)   == false
   *   GAME_MASTER.atLeast(ADMIN)  == true
   * </pre>
   */
  public boolean atLeast(InstanceRole minimum) {
    return this.ordinal() >= minimum.ordinal();
  }
}
