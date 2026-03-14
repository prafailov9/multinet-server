package com.ntros.model.world.wator;

import com.ntros.model.world.wator.component.AgentComponent;
import com.ntros.model.world.wator.component.EnergyComponent;
import com.ntros.model.world.wator.component.GenomeComponent;
import com.ntros.model.world.wator.component.HealthComponent;
import com.ntros.model.world.wator.component.PlantComponent;
import com.ntros.model.world.wator.component.Position2f;
import com.ntros.model.world.wator.component.VelocityComponent;
import com.ntros.model.world.wator.component.VisionComponent;
import com.ntros.model.world.wator.system.BrainSystem;
import com.ntros.model.world.wator.system.DeathSystem;
import com.ntros.model.world.wator.system.EnergySystem;
import com.ntros.model.world.wator.system.MovementSystem;
import com.ntros.model.world.wator.system.PlantSystem;
import com.ntros.model.world.wator.system.ReproductionSystem;
import com.ntros.model.world.wator.system.VisionSystem;
import com.ntros.model.world.wator.system.WaTorSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * ECS container for the Wa-Tor simulation.
 *
 * <h3>System execution order (per tick)</h3>
 * <ol>
 *   <li>{@link com.ntros.model.world.wator.spatial.SpatialHash#rebuild} — update spatial index</li>
 *   <li>{@link VisionSystem} — populate sensor data from spatial hash</li>
 *   <li>{@link BrainSystem}  — evaluate NN, write motor commands to VelocityComponent</li>
 *   <li>{@link MovementSystem} — integrate velocity into position</li>
 *   <li>{@link EnergySystem} — drain energy, process eating interactions</li>
 *   <li>{@link ReproductionSystem} — accrue reproduction progress, queue offspring</li>
 *   <li>{@link PlantSystem} — grow plants, queue spread and spontaneous spawns</li>
 *   <li>{@link DeathSystem} — mark dead entities, queue food-on-death spawns</li>
 *   <li>{@link #flushSpawns} — materialise all queued spawns into component arrays</li>
 *   <li>{@link #compactRemovals} — reclaim dead slots (null-out component references)</li>
 * </ol>
 */
@Slf4j
public class WaTorWorld {

  // ── Simulation constants ──────────────────────────────────────────────────

  private static final float PREDATOR_MAX_HP     = 100f;
  private static final float PREDATOR_MAX_ENERGY = 150f;
  private static final float PREDATOR_RAY_LENGTH = 80f;
  private static final float PREDATOR_FOV        = (float) Math.PI;          // 180°

  private static final float PREY_MAX_HP         = 80f;
  private static final float PREY_MAX_ENERGY     = 120f;
  private static final float PREY_RAY_LENGTH     = 70f;
  private static final float PREY_FOV            = (float) (Math.PI * 4 / 3); // 240°

  private static final float PLANT_INITIAL_SIZE  = 1f;

  // ── State & systems ───────────────────────────────────────────────────────

  private final WaTorWorldState state;
  private final List<WaTorSystem> systems = new ArrayList<>();

  public WaTorWorld(String worldName) {
    this.state = new WaTorWorldState(worldName);
    systems.add(new VisionSystem());
    systems.add(new BrainSystem());
    systems.add(new MovementSystem());
    systems.add(new EnergySystem());
    systems.add(new ReproductionSystem());
    systems.add(new PlantSystem());
    systems.add(new DeathSystem());
  }

  // ── Public API ────────────────────────────────────────────────────────────

  public WaTorWorldState getState() {
    return state;
  }

  /**
   * Advances the simulation by one tick.
   *
   * @param dt elapsed time in seconds (typically 1/120 for 120 Hz)
   */
  public void tick(float dt) {
    // First rebuild: pre-movement positions — consumed by VisionSystem (sense → decide → act).
    state.spatialHash.rebuild(state);

    for (WaTorSystem system : systems) {
      system.tick(state, dt);

      // Second rebuild: after MovementSystem has updated all positions, rebuild so that
      // EnergySystem's interaction queries (INTERACTION_RADIUS = 6 wu) use current positions.
      // Without this, entities that moved INTO interaction range this tick are missed because
      // the hash still maps them to their pre-movement buckets.
      if (system instanceof MovementSystem) {
        state.spatialHash.rebuild(state);
      }
    }

    flushSpawns();
    compactRemovals();
  }

  // ── Entity spawning ───────────────────────────────────────────────────────

  /** Spawns a predator at a random position with a random genome. */
  public int spawnPredator() {
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    return spawnCreature(EntityType.PREDATOR,
        rng.nextFloat() * WaTorWorldState.WIDTH,
        rng.nextFloat() * WaTorWorldState.HEIGHT,
        new GenomeComponent(),
        PREDATOR_MAX_ENERGY * 0.7f);
  }

  /** Spawns a prey at a random position with a random genome. */
  public int spawnPrey() {
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    return spawnCreature(EntityType.PREY,
        rng.nextFloat() * WaTorWorldState.WIDTH,
        rng.nextFloat() * WaTorWorldState.HEIGHT,
        new GenomeComponent(),
        PREY_MAX_ENERGY * 0.7f);
  }

  /** Spawns a plant at the given world-unit coordinates. */
  public int spawnPlant(float x, float y) {
    int id = state.allocate(EntityType.PLANT);
    state.positions.set(id, new Position2f(x, y));
    state.plants.set(id, new PlantComponent(PLANT_INITIAL_SIZE));
    return id;
  }

  // ── Private spawn helpers ─────────────────────────────────────────────────

  private int spawnCreature(EntityType type, float x, float y,
      GenomeComponent genome, float initialEnergy) {

    boolean isPredator = type == EntityType.PREDATOR;
    float maxHp     = isPredator ? PREDATOR_MAX_HP     : PREY_MAX_HP;
    float maxEnergy = isPredator ? PREDATOR_MAX_ENERGY : PREY_MAX_ENERGY;
    float rayLen    = isPredator ? PREDATOR_RAY_LENGTH  : PREY_RAY_LENGTH;
    float fov       = isPredator ? PREDATOR_FOV         : PREY_FOV;

    int id = state.allocate(type);
    float angle = (float) (ThreadLocalRandom.current().nextDouble() * 2 * Math.PI);

    state.positions.set(id,   new Position2f(x, y));
    state.velocities.set(id,  new VelocityComponent(angle, 0f));
    state.agents.set(id,      new AgentComponent());
    state.energies.set(id,    new EnergyComponent(initialEnergy, maxEnergy));
    state.healths.set(id,     new HealthComponent(maxHp, maxHp));
    state.visions.set(id,     new VisionComponent(rayLen, fov));
    state.genomes.set(id,     genome);
    return id;
  }

  private int spawnFood(float x, float y, float nutrition) {
    int id = state.allocate(EntityType.FOOD);
    state.positions.set(id, new Position2f(x, y));
    // energy.current = TTL countdown; energy.max = nutrition value
    state.energies.set(id, new EnergyComponent(DeathSystem.FOOD_TTL_TICKS, nutrition));
    return id;
  }

  // ── End-of-tick maintenance ───────────────────────────────────────────────

  /**
   * Materialises all queued {@link SpawnRequest}s into component arrays.
   * Called after all systems have finished so system iteration is never invalidated
   * by mid-tick structural changes.
   */
  private void flushSpawns() {
    for (SpawnRequest req : state.pendingSpawns) {
      switch (req.type()) {
        case PREDATOR -> spawnCreature(EntityType.PREDATOR, req.x(), req.y(),
            req.genome() != null ? req.genome() : new GenomeComponent(),
            req.energyValue());
        case PREY     -> spawnCreature(EntityType.PREY, req.x(), req.y(),
            req.genome() != null ? req.genome() : new GenomeComponent(),
            req.energyValue());
        case PLANT    -> spawnPlant(req.x(), req.y());
        case FOOD     -> spawnFood(req.x(), req.y(), req.energyValue());
      }
    }
    if (!state.pendingSpawns.isEmpty()) {
      log.debug("[WaTorWorld] Flushed {} spawn(s).", state.pendingSpawns.size());
      state.pendingSpawns.clear();
    }
  }

  /**
   * Null-out component slots for entities that died this tick.
   * Does NOT compact the array (indices remain stable across ticks).
   * Compaction (index reassignment) is deferred until the array grows
   * significantly fragmented — not yet implemented, as 200–300 entities
   * with bounded lifetime keeps fragmentation low.
   */
  private void compactRemovals() {
    for (int id : state.pendingRemovals) {
      state.positions.set(id,  null);
      state.velocities.set(id, null);
      state.agents.set(id,     null);
      state.energies.set(id,   null);
      state.healths.set(id,    null);
      state.visions.set(id,    null);
      state.genomes.set(id,    null);
      state.plants.set(id,     null);
    }
    if (!state.pendingRemovals.isEmpty()) {
      log.debug("[WaTorWorld] Removed {} entity slot(s).", state.pendingRemovals.size());
      state.pendingRemovals.clear();
    }
  }
}
