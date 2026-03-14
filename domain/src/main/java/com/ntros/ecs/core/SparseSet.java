package com.ntros.ecs.core;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Sparse-set component store — best for components held by a minority of entities.
 *
 * <h3>Data structure</h3>
 * <pre>
 *  sparse[entityId] = index into dense arrays  (-1 = absent)
 *  denseIds[i]      = entity ID of the i-th packed entry
 *  denseValues[i]   = component of the i-th packed entry
 * </pre>
 *
 * <p>All operations are O(1). {@link #forEach} iterates only the dense (packed) segment, so
 * it visits only present entities — perfect for rare components such as {@code PlayerTagComp}
 * on a handful of observer-tagged boids.
 *
 * @param <T> the component type stored
 */
public class SparseSet<T extends Component> implements ComponentStore<T> {

  private static final int INITIAL_DENSE = 16;
  private static final int INITIAL_SPARSE = 64;

  /** sparse[entityId] = index in dense arrays, or -1 if absent. */
  private int[] sparse;

  /** Packed entity IDs of present entries. */
  private int[] denseIds;

  /** Packed component values aligned with {@code denseIds}. */
  private Object[] denseValues;

  /** Number of entries currently stored in the dense segment. */
  private int count = 0;

  public SparseSet() {
    sparse = new int[INITIAL_SPARSE];
    denseIds = new int[INITIAL_DENSE];
    denseValues = new Object[INITIAL_DENSE];
    Arrays.fill(sparse, -1);
  }

  // ── ComponentStore ─────────────────────────────────────────────────────────

  @Override
  public void set(EntityId id, T component) {
    int eid = id.id();
    ensureSparse(eid);
    if (sparse[eid] != -1) {
      // Update in place.
      denseValues[sparse[eid]] = component;
      return;
    }
    ensureDense(count);
    sparse[eid] = count;
    denseIds[count] = eid;
    denseValues[count] = component;
    count++;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T get(EntityId id) {
    int eid = id.id();
    if (eid >= sparse.length || sparse[eid] == -1) {
      return null;
    }
    return (T) denseValues[sparse[eid]];
  }

  @Override
  public boolean has(EntityId id) {
    int eid = id.id();
    return eid < sparse.length && sparse[eid] != -1;
  }

  @Override
  public void remove(EntityId id) {
    int eid = id.id();
    if (eid >= sparse.length || sparse[eid] == -1) {
      return;
    }
    int denseIdx = sparse[eid];
    int lastEid = denseIds[count - 1];

    // Swap with last packed entry, then shrink count.
    denseIds[denseIdx] = lastEid;
    denseValues[denseIdx] = denseValues[count - 1];
    sparse[lastEid] = denseIdx;

    denseValues[count - 1] = null; // help GC
    sparse[eid] = -1;
    count--;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void forEach(BiConsumer<EntityId, T> action) {
    for (int i = 0; i < count; i++) {
      action.accept(new EntityId(denseIds[i]), (T) denseValues[i]);
    }
  }

  // ── Internal ───────────────────────────────────────────────────────────────

  private void ensureSparse(int eid) {
    if (eid >= sparse.length) {
      int newLen = Math.max(sparse.length * 2, eid + 1);
      int[] grown = new int[newLen];
      System.arraycopy(sparse, 0, grown, 0, sparse.length);
      Arrays.fill(grown, sparse.length, newLen, -1);
      sparse = grown;
    }
  }

  private void ensureDense(int requiredIndex) {
    if (requiredIndex >= denseIds.length) {
      int newLen = Math.max(denseIds.length * 2, requiredIndex + 1);
      denseIds = Arrays.copyOf(denseIds, newLen);
      denseValues = Arrays.copyOf(denseValues, newLen);
    }
  }
}
