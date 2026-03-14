package com.ntros.model.entity.config;

/**
 * Describes when a world instance starts and stops its simulation clock.
 *
 * <p>This is a static property of the <em>world type</em>, not a runtime configuration value.
 * It lives in {@link WorldCapabilities} rather than in {@code InstanceSettings}.
 *
 * <table border="1">
 *   <tr><th>Policy</th><th>Clock starts when&hellip;</th><th>Clock stops when&hellip;</th><th>Examples</th></tr>
 *   <tr>
 *     <td>{@code PLAYER_DRIVEN}</td>
 *     <td>The first player joins</td>
 *     <td>The last player leaves</td>
 *     <td>Arena, solo worlds</td>
 *   </tr>
 *   <tr>
 *     <td>{@code ORCHESTRATION_DRIVEN}</td>
 *     <td>The first {@code ORCHESTRATE} command arrives</td>
 *     <td>Never automatically (only on server reset/stop)</td>
 *     <td>Game of Life, Falling Sand</td>
 *   </tr>
 *   <tr>
 *     <td>{@code AUTONOMOUS}</td>
 *     <td>Server bootstrap</td>
 *     <td>Server shutdown</td>
 *     <td>Wa-Tor, traffic simulation (future)</td>
 *   </tr>
 * </table>
 */
public enum LifecyclePolicy {

  /**
   * Clock is driven by player presence.
   * Starts on first join, stops when all players leave.
   */
  PLAYER_DRIVEN,

  /**
   * Clock is driven by orchestration commands.
   * Starts on the first {@code ORCHESTRATE} action; players join as observers only.
   */
  ORCHESTRATION_DRIVEN,

  /**
   * Clock runs autonomously from server boot, independent of players.
   */
  AUTONOMOUS
}
