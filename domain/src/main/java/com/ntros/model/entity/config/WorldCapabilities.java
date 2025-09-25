package com.ntros.model.entity.config;

public record WorldCapabilities(
    boolean supportsPlayers,
    boolean supportsOrchestrator,
    boolean hasAIEntities,
    boolean isDeterministic
) { }