package com.ntros.command;

import static com.ntros.protocol.Message.errorMsg;
import static com.ntros.protocol.Message.welcomeMsg;

import com.ntros.command.access.InstanceAccessController;
import com.ntros.command.exception.JoinCmdException;
import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.model.entity.config.access.InstanceRole;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.persistence.db.PersistenceContext;
import com.ntros.persistence.model.ClientRecord;
import com.ntros.persistence.model.InstanceRoleRecord;
import com.ntros.protocol.Message;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Processes a JOIN request: validates the client, routes them to the requested world instance,
 * and loads their persisted {@link InstanceRole} for that world into {@link SessionContext}.
 *
 * <p>If the client has no prior role assignment for this world, they receive the default
 * {@link InstanceRole#PLAYER} role, which is also persisted so subsequent JOINs restore it.
 */
@Slf4j
public class JoinCommand extends AbstractCommand {

  @Override
  public Message execute(Message message, Session session) {
    try {
      SessionContext ctx = session.getSessionContext();
      checkAuthenticated(message, ctx);

      String player = resolvePlayer(message);
      Instance instance = resolveInstance(message);
      if (instance == null) {
        log.error("[JOIN] Instance not found. Full message: {}", message);
        return errorMsg(String.format("WORLD_NOT_FOUND for player: %s", player), ctx.getUsername());
      }

      // Access guard — visibility and capacity check
      Optional<String> denied = InstanceAccessController.checkJoin(ctx, instance);
      if (denied.isPresent()) {
        log.warn("[JOIN] access denied for player '{}': {}", player, denied.get());
        return errorMsg(denied.get());
      }

      WorldResult result = instance.joinAsync(new JoinRequest(player)).join();
      if (result.success()) {
        ctx.setEntityId(result.playerName());
        ctx.setWorldId(result.worldName());
        ctx.setAuthenticated(true);
        ctx.setJoinedAt(OffsetDateTime.now());

        // Load (or assign default) instance role for this world.
        loadInstanceRoles(ctx, result.worldName());

        return welcomeMsg(result.playerName());
      }
      return errorMsg(result.reason());
    } catch (JoinCmdException | IllegalArgumentException ex) {
      log.error("[JOIN] Exception: {}", ex.getMessage(), ex);
      return errorMsg(ex.getMessage());
    }
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private void checkAuthenticated(Message message, SessionContext ctx) {
    Optional<ClientRecord> clientRecord = PersistenceContext.clients()
        .findByUsername(message.args().getFirst());
    if (!ctx.isAuthenticated()) {
      if (clientRecord.isEmpty()) {
        throw new JoinCmdException("client not authenticated");
      }
      ctx.setAuthenticated(true);
    }
  }

  /**
   * Finds the client's persisted role for {@code worldName} and stores it in {@code ctx}.
   * If no assignment exists yet, defaults to {@link InstanceRole#PLAYER} and persists it
   * so future JOINs to the same world restore the correct role.
   */
  private void loadInstanceRoles(SessionContext ctx, String worldName) {
    String username = ctx.getUsername();
    if (username == null || username.isBlank()) {
      applyRole(ctx, InstanceRole.PLAYER);
      return;
    }

    try {
      Optional<InstanceRoleRecord> record =
          PersistenceContext.clientsInstanceRoles()
              .findRoleForClientInWorld(username, worldName);

      InstanceRole role;
      if (record.isPresent()) {
        role = parseInstanceRole(record.get().getInstanceRoleName(), username, worldName);
      } else {
        role = InstanceRole.PLAYER;
        PersistenceContext.clientsInstanceRoles()
            .assignRole(username, worldName, InstanceRole.PLAYER.name());
        log.info("[JOIN] Assigned default PLAYER role to '{}' in world '{}'.", username, worldName);
      }
      applyRole(ctx, role);

    } catch (Exception ex) {
      log.warn("[JOIN] Could not load instance role for '{}' in '{}': {}. Using PLAYER.",
          username, worldName, ex.getMessage());
      applyRole(ctx, InstanceRole.PLAYER);
    }
  }

  private void applyRole(SessionContext ctx, InstanceRole role) {
    ctx.setInstanceRole(role);           // single active role for the current world
    ctx.setInstanceRoles(List.of(role)); // list form for future access-controller use
  }

  private InstanceRole parseInstanceRole(String name, String username, String worldName) {
    try {
      return InstanceRole.valueOf(name);
    } catch (IllegalArgumentException ex) {
      log.warn("[JOIN] Unknown InstanceRole '{}' for '{}' in '{}', defaulting to PLAYER.",
          name, username, worldName);
      return InstanceRole.PLAYER;
    }
  }

  private String resolvePlayer(Message message) {
    String playerName = message.args().getFirst();
    if (playerName == null || playerName.isEmpty()) {
      logAndThrow("[JOIN] No player name given.");
    }
    return playerName;
  }

  private Instance resolveInstance(Message message) {
    String worldName = message.args().size() >= 2 ? message.args().get(1) : null;
    if (worldName == null) {
      log.error("[JOIN] No world name in message {}", message);
      throw new JoinCmdException("Message missing world-name argument");
    }
    return Instances.getInstance(worldName);
  }
}
