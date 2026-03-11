package com.ntros.persistence.model;

import java.time.Instant;

/**
 * Lightweight DTO that mirrors the {@code worlds} database row.
 *
 * @param name      unique world name (primary key)
 * @param type      world type string — {@code "GRID"}, {@code "OPEN"}, etc.
 * @param width     X dimension
 * @param height    Y dimension
 * @param depth     Z dimension (0 for 2D grid worlds)
 * @param createdAt when this world was first registered
 */
public record WorldRecord(
    String  name,
    String  type,
    int     width,
    int     height,
    int     depth,
    Instant createdAt
) {

}
