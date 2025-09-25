package com.ntros.model.entity.config.access;

public record WorldConfig(
    int maxPlayers,
    boolean requiresOrchestrator,
    Visibility visibility,
    boolean autoStartOnPlayerJoin
) { }