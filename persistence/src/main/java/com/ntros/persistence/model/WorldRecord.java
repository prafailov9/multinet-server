package com.ntros.persistence.model;

import java.time.Instant;

/**
 * Lightweight DTO that mirrors the {@code worlds} database row.
 *
 * <p>This is the single source of truth for world configuration. All runtime
 * world objects ({@code WorldConnector}) are built from this record.
 *
 * @param name          unique world name (primary key)
 * @param engineType    engine to use — {@code "GRID"} for solid grid worlds, {@code "GOL"} for
 *                      Game of Life worlds
 * @param width         X dimension
 * @param height        Y dimension
 * @param depth         Z dimension (0 for 2D grid worlds)
 * @param multiplayer   whether multiple players can join simultaneously
 * @param orchestrated  whether the world accepts ORCHESTRATE commands
 * @param hasAi         whether an AI agent runs in this world
 * @param deterministic whether the world evolves deterministically (same seed → same result)
 * @param createdAt     when this world was first registered
 */
public record WorldRecord(
    String name,
    String engineType,
    int width,
    int height,
    int depth,
    boolean multiplayer,
    boolean orchestrated,
    boolean hasAi,
    boolean deterministic,
    Instant createdAt
) {

}
