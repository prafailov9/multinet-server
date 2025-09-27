package com.ntros.command.impl;

import com.ntros.message.SessionContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractCommand implements Command {

  protected void validateContext(SessionContext context) {
    log.info("Validating client info...");
    if (!context.isAuthenticated()) {
      logAndThrow("User not authenticated.");
    }
    if (context.getSessionId() <= 0) {
      logAndThrow("No session exists for caller.");
    }
    if (context.getEntityId() == null || context.getEntityId().isEmpty()) {
      logAndThrow("No playerId associated with caller.");
    }

    if (context.getWorldName() == null || context.getWorldName().isEmpty()) {
      logAndThrow("No world assigned.");
    }
  }

  void logAndThrow(String err) {
    log.error(err);
    throw new IllegalArgumentException(err);
  }

}
