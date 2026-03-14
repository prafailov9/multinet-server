package com.ntros.command.access;

import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.model.entity.config.LifecyclePolicy;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.entity.config.access.InstanceRole;
import com.ntros.model.entity.config.access.InstanceSettings;
import com.ntros.model.entity.config.access.InstanceVisibility;
import com.ntros.model.entity.config.access.SystemRole;
import java.util.Optional;

/**
 * Centralises access-control decisions for world-instance operations.
 *
 * <p>Commands call the relevant {@code check*} method <em>before</em> delegating to an instance.
 * The controller returns {@code Optional.empty()} when access is granted, or a non-empty
 * {@code Optional<String>} containing the human-readable rejection reason.
 *
 * <p>{@link SystemRole#ROOT} bypasses every check — ROOT is unrestricted at the instance level.
 */
public final class InstanceAccessController {

  /**
   * Checks whether {@code ctx} may join the given {@code instance}.
   *
   * <ul>
   *   <li>ROOT bypasses all checks.</li>
   *   <li>PRIVATE instances reject non-ROOT callers.</li>
   *   <li>Full instances (at max player capacity) reject any caller.</li>
   *   <li>Stopped instances reject callers unless the lifecycle policy permits joining:
   *       {@code PLAYER_DRIVEN} (auto-starts on first join) and
   *       {@code ORCHESTRATION_DRIVEN} (observer join, clock starts on ORCHESTRATE) are allowed;
   *       {@code AUTONOMOUS} instances must already be running.</li>
   * </ul>
   *
   * @return empty if access is granted, or the rejection reason
   */
  public static Optional<String> checkJoin(SessionContext ctx, Instance instance) {
    if (isRoot(ctx)) {
      return Optional.empty();
    }
    InstanceSettings settings = instance.getSettings();
    if (settings.instanceVisibility() == InstanceVisibility.PRIVATE) {
      return Optional.of("Instance is private.");
    }
    if (instance.getActiveSessionsCount() >= settings.maxPlayers()) {
      return Optional.of(
          "Instance is full (" + settings.maxPlayers() + " player cap).");
    }
    if (!instance.isRunning()) {
      LifecyclePolicy policy = instance.getWorldConnector().getCapabilities().lifecyclePolicy();
      boolean joinableWhenStopped =
          policy == LifecyclePolicy.ORCHESTRATION_DRIVEN
          || (policy == LifecyclePolicy.PLAYER_DRIVEN && instance.getActiveSessionsCount() == 0);
      if (!joinableWhenStopped) {
        return Optional.of(String.format(
            "Instance is not running. LifecyclePolicy=%s does not permit joining a stopped instance.",
            policy));
      }
    }
    return Optional.empty();
  }

  /**
   * Checks whether {@code ctx} may send an ORCHESTRATE command to the given {@code instance}.
   *
   * <ul>
   *   <li>ROOT bypasses all checks.</li>
   *   <li>Rejects if the world does not support orchestration.</li>
   *   <li>Rejects if the caller's instance role is below
   *       {@link WorldCapabilities#minimumOrchestratorRole()}.</li>
   * </ul>
   *
   * @return empty if access is granted, or the rejection reason
   */
  public static Optional<String> checkOrchestrate(SessionContext ctx, Instance instance) {
    if (isRoot(ctx)) {
      return Optional.empty();
    }
    WorldCapabilities caps = instance.getWorldConnector().getCapabilities();
    if (!caps.supportsOrchestrator()) {
      return Optional.of("World does not support orchestration.");
    }
    InstanceRole required = caps.minimumOrchestratorRole();
    InstanceRole actual = ctx.getInstanceRole();
    if (actual == null || !actual.atLeast(required)) {
      return Optional.of(String.format(
          "ORCHESTRATE requires %s role or above (you have %s).", required, actual));
    }
    return Optional.empty();
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private static boolean isRoot(SessionContext ctx) {
    return ctx.getSystemRole() == SystemRole.ROOT;
  }

  private InstanceAccessController() {
    // static utility
  }
}
