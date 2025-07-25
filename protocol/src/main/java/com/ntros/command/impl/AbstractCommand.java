package com.ntros.command.impl;

import com.ntros.message.ProtocolContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractCommand implements Command {

  protected void validateContext(ProtocolContext context) {
    log.info("Validating client info...");
    if (!context.isAuthenticated()) {
      logAndThrow("User not authenticated.");
    }
    if (context.getSessionId() == null) {
      logAndThrow("No session exists for caller.");
    }
    if (context.getPlayerId() == null || context.getPlayerId().isEmpty()) {
      logAndThrow("No playerId associated with caller.");
    }

    if (context.getWorldId() == null || context.getWorldId().isEmpty()) {
      logAndThrow("No world assigned.");
    }
  }

  void logAndThrow(String err) {
    log.error(err);
    throw new IllegalArgumentException(err);
  }

}
