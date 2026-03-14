package com.ntros.command;

import static com.ntros.protocol.Message.authSuccess;

import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.model.entity.config.access.SystemRole;
import com.ntros.persistence.db.PersistenceContext;
import com.ntros.persistence.model.ClientRecord;
import com.ntros.persistence.model.SystemRoleRecord;
import com.ntros.protocol.Message;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticates the client at the transport level.
 *
 * <p>If the client already has a record in the database, their persisted {@link SystemRole}
 * is loaded into {@link SessionContext} so that downstream commands (e.g.
 * {@link OrchestratorCommand}) can enforce the correct privilege level without a second
 * round-trip.
 */
@Slf4j
public class AuthCommand extends AbstractCommand {

  private static final String CLIENT_AUTHENTICATED = "CLIENT_AUTHENTICATED";
  private static final String NOT_IN_WORLD = "NOT_IN_WORLD";

  @Override
  public Message execute(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    if (ctx.isAuthenticated()) {
      return Message.errorMsg("User already authenticated");
    }

    ctx.setAuthenticated(true);

    if (isNotInWorld(ctx)) {
      ctx.setEntityId(CLIENT_AUTHENTICATED);
      ctx.setWorldId(NOT_IN_WORLD);
    }

    // Load the persisted system role for this session's username.
    // Falls back to USER if the username isn't known yet (e.g. pre-REGISTER flow).
    loadSystemRole(ctx);

    return authSuccess(ctx.getSessionId());
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private boolean isNotInWorld(SessionContext ctx) {
    return ctx.getWorldName() == null || ctx.getWorldName().isBlank()
        || ctx.getEntityId() == null || ctx.getEntityId().isBlank();
  }

  private void loadSystemRole(SessionContext ctx) {
    String username = ctx.getUsername();
    if (username == null || username.isBlank()) {
      ctx.setSystemRole(SystemRole.USER);
      return;
    }

    try {
      PersistenceContext.clients()
          .findByUsername(username)
          .map(ClientRecord::systemRoleRecord)
          .map(SystemRoleRecord::getSystemRoleName)
          .map(name -> {
            try {
              return SystemRole.valueOf(name);
            } catch (IllegalArgumentException ex) {
              log.warn("[AuthCommand] Unknown SystemRole name '{}' for user '{}', defaulting to USER.",
                  name, username);
              return SystemRole.USER;
            }
          })
          .ifPresentOrElse(
              ctx::setSystemRole,
              () -> ctx.setSystemRole(SystemRole.USER));
    } catch (Exception ex) {
      log.warn("[AuthCommand] Could not load system role for '{}': {}. Defaulting to USER.",
          username, ex.getMessage());
      ctx.setSystemRole(SystemRole.USER);
    }
  }
}
