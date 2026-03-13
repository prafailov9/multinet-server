package com.ntros.model.world.wator;

import com.ntros.model.world.wator.component.AgentComponent;
import com.ntros.model.world.wator.component.EnergyComponent;
import com.ntros.model.world.wator.component.HealthComponent;
import com.ntros.model.world.wator.component.PlantComponent;
import com.ntros.model.world.wator.component.Position2f;
import com.ntros.model.world.wator.component.VelocityComponent;
import com.ntros.model.world.wator.component.VisionComponent;
import com.ntros.model.world.wator.component.GenomeComponent;
import com.ntros.model.world.wator.spatial.SpatialHash;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mutable ECS world state for the Wa-Tor predator-prey simulation.
 *
 * <h3>Entity model</h3>
 * Entities are identified by a dense integer ID (index into the component arrays).
 * Three logical entity types share the same ID space:
 * <ul>
 *   <li><b>Predator</b> — has AgentComponent, Position2f, VelocityComponent, EnergyComponent,
 *       HealthComponent, VisionComponent, GenomeComponent</li>
 *   <li><b>Prey</b>     — same component set as predator</li>
 *   <li><b>Plant</b>    — has Position2f, PlantComponent</li>
 *   <li><b>Food</b>     — has Position2f, EnergyComponent (nutrition value), lifetime counter</li>
 * </ul>
 *
 * <h3>Component storage</h3>
 * Each component type is stored in its own {@link ArrayList}.  Components at the same index
 * belong to the same entity.  A {@code null} slot means the entity at that index does not
 * have that component (or has been removed — compaction happens at end-of-tick).
 *
 * <h3>Thread-safety</h3>
 * All mutations must be made from the actor thread only.  The {@link #pendingSpawns} list is
 * the single exception: it is a {@link CopyOnWriteArrayList} so that the {@code DeathSystem}
 * can queue spawn-on-death events while iterating other component lists.
 */
public class WaTorWorldState {

  // ── World metadata ────────────────────────────────────────────────────────

  private final String worldName;

  /** World width in continuous units (512). */
  public static final float WIDTH  = 512f;
  /** World height in continuous units (512). */
  public static final float HEIGHT = 512f;

  // ── Component lists (parallel arrays — index == entity ID) ───────────────

  // In ECS the component arrays ARE the public data — systems are the behaviour.
  // All fields are public so systems in sub-packages can read/write without accessors.
  public final List<Position2f>        positions   = new ArrayList<>();
  public final List<VelocityComponent> velocities  = new ArrayList<>();
  public final List<AgentComponent>    agents      = new ArrayList<>();
  public final List<EnergyComponent>   energies    = new ArrayList<>();
  public final List<HealthComponent>   healths     = new ArrayList<>();
  public final List<VisionComponent>   visions     = new ArrayList<>();
  public final List<GenomeComponent>   genomes     = new ArrayList<>();
  public final List<PlantComponent>    plants      = new ArrayList<>();

  /** Entity type tag — never null once an entity is alive. */
  public final List<EntityType> types = new ArrayList<>();

  /** {@code true} if the slot is active (not pending removal). */
  public final List<Boolean> alive = new ArrayList<>();

  // ── Spatial acceleration ──────────────────────────────────────────────────

  /** Spatial hash rebuilt from {@link #positions} at the start of each tick. */
  public final SpatialHash spatialHash = new SpatialHash();

  // ── Deferred mutation queues ──────────────────────────────────────────────

  /**
   * Spawn requests queued during a tick (e.g. food-on-death, reproduction offspring).
   * Flushed into component arrays at the end of each tick by {@link WaTorWorld}.
   */
  public final CopyOnWriteArrayList<SpawnRequest> pendingSpawns = new CopyOnWriteArrayList<>();

  /** IDs of entities that died this tick and must be removed at end-of-tick. */
  public final List<Integer> pendingRemovals = new ArrayList<>();

  // ── Entity ID counter ─────────────────────────────────────────────────────

  private int nextId = 0;

  // ── Constructor ───────────────────────────────────────────────────────────

  public WaTorWorldState(String worldName) {
    this.worldName = worldName;
  }

  // ── Accessors ─────────────────────────────────────────────────────────────

  public String worldName() {
    return worldName;
  }

  public int entityCount() {
    return types.size();
  }

  /** Allocates the next entity ID and pads all component arrays with null slots. */
  int allocate(EntityType type) {
    int id = nextId++;
    positions.add(null);
    velocities.add(null);
    agents.add(null);
    energies.add(null);
    healths.add(null);
    visions.add(null);
    genomes.add(null);
    plants.add(null);
    types.add(type);
    alive.add(true);
    return id;
  }

  /** Returns an unmodifiable view of the active entity types for snapshot/serialisation. */
  public List<EntityType> getTypes() {
    return Collections.unmodifiableList(types);
  }

  public SpatialHash getSpatialHash() {
    return spatialHash;
  }
}
