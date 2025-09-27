package com.ntros.model.entity.config.access;

public record InstanceConfig(
    int maxPlayers,
    boolean requiresOrchestrator,
    Visibility visibility,
    boolean autoStartOnPlayerJoin
) { }