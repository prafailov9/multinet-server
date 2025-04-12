package com.ntros.command.impl;

import com.ntros.message.ProtocolContext;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(AbstractCommand.class.getName());


    protected void validateContext(ProtocolContext context) {
        LOGGER.log(Level.INFO, "validating context...");
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
        LOGGER.log(Level.SEVERE, err);
        throw new RuntimeException(err);
    }

}
