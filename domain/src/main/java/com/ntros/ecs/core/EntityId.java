package com.ntros.ecs.core;

/**
 * Lightweight entity identifier. All component stores are keyed by this value.
 *
 * <p>Implemented as a plain {@code record} so comparisons use {@code id} equality, not
 * identity, and it can be used as a {@code HashMap} key without boxing.
 */
public record EntityId(int id) {

}
