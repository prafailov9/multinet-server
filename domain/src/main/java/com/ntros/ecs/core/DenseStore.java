package com.ntros.ecs.core;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Array-backed component store — best for components held by most entities.
 *
 * <p>Internally a plain {@code Object[]} indexed by {@link EntityId#id()}.
 * A {@code null} slot means "not present"; set/get/has/remove are all O(1).
 * The array grows on demand (doubles capacity) — no manual sizing required.
 *
 * <p>{@link #forEach} scans the full array and skips {@code null} slots, so its cost is
 * proportional to the highest entity ID seen, not the number of live entities. For dense
 * entity populations this is cache-sequential and very fast.
 *
 * @param <T> the component type stored
 */
public class DenseStore<T extends Component> implements ComponentStore<T> {

  private static final int INITIAL_CAPACITY = 64;

  private Object[] data = new Object[INITIAL_CAPACITY];

  // ── ComponentStore ─────────────────────────────────────────────────────────

  @Override
  public void set(EntityId id, T component) {
    ensureCapacity(id.id());
    data[id.id()] = component;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T get(EntityId id) {
    int i = id.id();
    return i < data.length ? (T) data[i] : null;
  }

  @Override
  public boolean has(EntityId id) {
    int i = id.id();
    return i < data.length && data[i] != null;
  }

  @Override
  public void remove(EntityId id) {
    int i = id.id();
    if (i < data.length) {
      data[i] = null;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void forEach(BiConsumer<EntityId, T> action) {
    for (int i = 0; i < data.length; i++) {
      if (data[i] != null) {
        action.accept(new EntityId(i), (T) data[i]);
      }
    }
  }

  // ── Internal ───────────────────────────────────────────────────────────────

  private void ensureCapacity(int index) {
    if (index >= data.length) {
      data = Arrays.copyOf(data, Math.max(data.length * 2, index + 1));
    }
  }
}
