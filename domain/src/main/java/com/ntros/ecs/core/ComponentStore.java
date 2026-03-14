package com.ntros.ecs.core;

import java.util.function.BiConsumer;

/**
 * Storage backend for a single component type.
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>{@link DenseStore} — array-backed; O(1) get/set; best when most entities carry the
 *       component (e.g. Position, Velocity).</li>
 *   <li>{@link SparseSet} — sparse/dense pair; O(1) everything; cache-friendly iteration over
 *       only present entries; best for rare components (e.g. PlayerTag on 2 of 500 boids).</li>
 * </ul>
 *
 * @param <T> the component type stored
 */
public interface ComponentStore<T extends Component> {

  /** Stores {@code component} for the given entity, replacing any existing value. */
  void set(EntityId id, T component);

  /** Returns the component for {@code id}, or {@code null} if absent. */
  T get(EntityId id);

  /** Returns {@code true} iff {@code id} has a component in this store. */
  boolean has(EntityId id);

  /** Removes the component for {@code id}; no-op if absent. */
  void remove(EntityId id);

  /** Iterates all present (id, component) pairs in unspecified order. */
  void forEach(BiConsumer<EntityId, T> action);
}
