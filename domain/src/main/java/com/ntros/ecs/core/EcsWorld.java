package com.ntros.ecs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central ECS registry.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Entity creation / destruction.</li>
 *   <li>Component store registration and lookup.</li>
 *   <li>System registration and ordered execution via {@link #tick}.</li>
 * </ul>
 *
 * <h3>Scale note</h3>
 * Designed for ~100–2 000 entities. There is no archetype graph — queries iterate the smaller
 * of two stores and probe the other. Sufficient for this server's workloads.
 *
 * <h3>Thread safety</h3>
 * Not thread-safe. All mutations must happen on the same thread (the server's actor thread).
 */
public class EcsWorld {

  private int nextId = 0;
  private final Map<Class<?>, ComponentStore<?>> stores = new HashMap<>();
  private final List<EcsSystem> systems = new ArrayList<>();

  // ── Entity lifecycle ───────────────────────────────────────────────────────

  /** Allocates a new unique entity ID. The entity starts with no components. */
  public EntityId create() {
    return new EntityId(nextId++);
  }

  /**
   * Removes the entity from every registered store.
   *
   * <p>O(number-of-store-types) — typically 3–5 for the boids simulation.
   */
  public void destroy(EntityId id) {
    stores.values().forEach(store -> store.remove(id));
  }

  /** Returns the number of entities created so far (including destroyed ones). */
  public int entityCount() {
    return nextId;
  }

  // ── Store registration ─────────────────────────────────────────────────────

  /**
   * Returns (creating if absent) a {@link DenseStore} for {@code type}.
   *
   * <p>Use for components expected on most entities (Position, Velocity).
   */
  @SuppressWarnings("unchecked")
  public <T extends Component> DenseStore<T> denseStore(Class<T> type) {
    return (DenseStore<T>) stores.computeIfAbsent(type, k -> new DenseStore<>());
  }

  /**
   * Returns (creating if absent) a {@link SparseSet} for {@code type}.
   *
   * <p>Use for components expected on few entities (PlayerTag, rare flags).
   */
  @SuppressWarnings("unchecked")
  public <T extends Component> SparseSet<T> sparseStore(Class<T> type) {
    return (SparseSet<T>) stores.computeIfAbsent(type, k -> new SparseSet<>());
  }

  /**
   * Returns the store for {@code type}, or {@code null} if none has been registered.
   */
  @SuppressWarnings("unchecked")
  public <T extends Component> ComponentStore<T> store(Class<T> type) {
    return (ComponentStore<T>) stores.get(type);
  }

  // ── System registration ────────────────────────────────────────────────────

  /** Appends a system to the execution list. Systems run in registration order. */
  public void addSystem(EcsSystem system) {
    systems.add(system);
  }

  // ── Tick ──────────────────────────────────────────────────────────────────

  /**
   * Runs all registered systems in order, passing this world and the elapsed time.
   *
   * @param dt elapsed seconds since the previous tick
   */
  public void tick(float dt) {
    for (EcsSystem system : systems) {
      system.update(this, dt);
    }
  }

  // ── Cross-component queries ────────────────────────────────────────────────

  /**
   * Iterates all entities that have both {@code typeA} and {@code typeB}, calling
   * {@code consumer} for each match.
   *
   * <p>Iterates the smaller store and probes the larger — simple and sufficient at this scale.
   */
  public <A extends Component, B extends Component> void query(
      Class<A> typeA, Class<B> typeB,
      TriConsumer<EntityId, A, B> consumer) {

    ComponentStore<A> storeA = store(typeA);
    ComponentStore<B> storeB = store(typeB);
    if (storeA == null || storeB == null) {
      return;
    }
    storeA.forEach((id, a) -> {
      B b = storeB.get(id);
      if (b != null) {
        consumer.accept(id, a, b);
      }
    });
  }
}
