package com.ntros.lifecycle;

import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Static registry for server lifecycle callbacks.
 *
 * <p>Provides hook points that modules at higher layers (e.g. {@code server}) can register
 * against without creating a compile-time dependency from {@code runtime} → {@code server} or
 * {@code runtime} → {@code persistence}. All defaults are safe no-ops.
 *
 * <h3>Available hooks</h3>
 * <ul>
 *   <li>{@link #onPlayerLeave} — fired when a player's session ends (disconnect or server stop).</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * Hooks are registered once at startup (before any session is created) and read many times
 * thereafter. {@code volatile} fields guarantee visibility without requiring synchronisation on
 * the hot path.
 */
@Slf4j
public final class LifecycleHooks {

  /**
   * Called whenever a player leaves a world.
   *
   * <p>Arguments: {@code (playerName, worldName)}.
   * Default: no-op.
   */
  private static volatile BiConsumer<String, String> onPlayerLeave = (name, world) -> {};

  private LifecycleHooks() {}

  // ── Registration (called once from ServerBootstrap) ───────────────────────

  /**
   * Replaces the {@code onPlayerLeave} hook.
   *
   * @param hook {@code (playerName, worldName)} consumer; must not be {@code null}
   */
  public static void setOnPlayerLeave(BiConsumer<String, String> hook) {
    if (hook == null) {
      throw new IllegalArgumentException("onPlayerLeave hook must not be null.");
    }
    onPlayerLeave = hook;
  }

  // ── Fire points (called from runtime commands) ────────────────────────────

  /**
   * Fires the {@code onPlayerLeave} hook. Safe to call even before the hook has been configured
   * (the default no-op is always in place).
   *
   * @param playerName name of the departing player
   * @param worldName  name of the world they are leaving
   */
  public static void firePlayerLeave(String playerName, String worldName) {
    try {
      onPlayerLeave.accept(playerName, worldName);
    } catch (Exception e) {
      // hooks must never crash the server — log and continue
      log.error("[LifecycleHooks] onPlayerLeave threw for player='{}' world='{}': {}",
          playerName, worldName, e.getMessage(), e);
    }
  }

  /** Resets all hooks to their default no-ops — intended for tests only. */
  public static void reset() {
    onPlayerLeave = (name, world) -> {};
  }
}
