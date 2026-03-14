package com.ntros.ecs.sim.boids;

import com.ntros.ecs.components.BoidComp;
import com.ntros.ecs.components.PositionComp;
import com.ntros.ecs.components.VelocityComp;
import com.ntros.ecs.core.ComponentStore;
import com.ntros.ecs.core.EcsWorld;
import com.ntros.ecs.core.EntityId;
import com.ntros.model.world.engine.core.SimulationGridEngine;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.EntityView;
import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.core.SimulationGridState;
import com.ntros.model.world.state.grid.GridSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * Boids flocking engine backed by the generic ECS framework.
 *
 * <h3>Architecture</h3>
 * The engine owns an {@link EcsWorld} containing {@link PositionComp}, {@link VelocityComp},
 * and {@link BoidComp} for each boid. A {@link BoidSpatialHash} is rebuilt at the start of
 * every tick to enable O(k) neighbourhood queries.
 *
 * <h3>Snapshot format</h3>
 * {@link #snapshot} returns a {@link GridSnapshot} with an empty terrain map and an entity map
 * keyed {@code "boid-N"} → {@link EntityView}(rounded int coordinates). The client's existing
 * entity-dot renderer displays them as moving dots without any protocol changes.
 *
 * <h3>Lifecycle</h3>
 * Boids is an {@code AUTONOMOUS} world — the clock runs from server bootstrap independently
 * of connected observers. {@link #orchestrate} is never called in normal operation
 * (the world capability has {@code supportsOrchestrator = false}); it is provided only for
 * completeness and returns a failure for any action.
 */
@Slf4j
public class BoidsEngine implements SimulationGridEngine {

  // ── Constants ──────────────────────────────────────────────────────────────

  /** Spatial hash cell size — should be ≥ the largest perception radius in {@link BoidComp}. */
  private static final float CELL_SIZE = 12f;

  /** Initial speed for newly spawned boids (world-units / second). */
  private static final float SPAWN_SPEED = 15f;

  // ── State ──────────────────────────────────────────────────────────────────

  private final int initialCount;
  private final float worldW;
  private final float worldH;

  private final EcsWorld ecsWorld = new EcsWorld();
  private final BoidSpatialHash spatialHash;

  /** Latest snapshot, rebuilt each tick and read (lock-free) by the broadcaster. */
  private volatile GridSnapshot latestSnapshot = new GridSnapshot(Map.of(), Map.of());

  // ── Constructor ────────────────────────────────────────────────────────────

  /**
   * Creates the engine and spawns {@code count} boids at random positions.
   *
   * @param count  number of boids to spawn
   * @param worldW world width in world-space units
   * @param worldH world height in world-space units
   */
  public BoidsEngine(int count, float worldW, float worldH) {
    this.initialCount = count;
    this.worldW = worldW;
    this.worldH = worldH;
    this.spatialHash = new BoidSpatialHash(worldW, worldH, CELL_SIZE);

    registerSystems();
    spawnBoids(count);

    log.info("[Boids] Engine initialised: {} boids, world {}×{}.", count, worldW, worldH);
  }

  // ── SimulationGridEngine ───────────────────────────────────────────────────

  /**
   * Advances the simulation by one tick:
   * <ol>
   *   <li>Rebuilds the spatial hash from current positions.</li>
   *   <li>Runs ECS systems (flocking → movement → bounds).</li>
   *   <li>Builds and caches the next {@link GridSnapshot}.</li>
   * </ol>
   */
  @Override
  public void applyIntents(GridState state) {
    // 1. Rebuild spatial hash from current positions.
    rebuildHash();

    // 2. Tick all ECS systems.
    float dt = 1f / 30f; // nominal delta-time at 30 TPS
    ecsWorld.tick(dt);

    // 3. Materialise snapshot for the broadcaster.
    latestSnapshot = buildSnapshot();
  }

  /**
   * Returns the pre-computed {@link GridSnapshot} from the most recent tick.
   *
   * <p>Called by the broadcaster after {@link #applyIntents} has already run on the actor
   * thread, so no locking is required — the field is {@code volatile}.
   */
  @Override
  public Object snapshot(GridState state) {
    return latestSnapshot;
  }

  @Override
  public String serialize(GridState state) {
    return buildJson(false);
  }

  @Override
  public String serializeOneLine(GridState state) {
    return buildJson(true);
  }

  @Override
  public void reset(GridState state) {
    ComponentStore<PositionComp> positions = ecsWorld.store(PositionComp.class);
    ComponentStore<VelocityComp> velocities = ecsWorld.store(VelocityComp.class);
    ComponentStore<BoidComp> boids = ecsWorld.store(BoidComp.class);
    if (positions != null) {
      positions.forEach((id, p) -> ecsWorld.destroy(id));
    }
    spawnBoids(initialCount);
    log.info("[Boids] Reset: respawned {} boids.", initialCount);
  }

  /**
   * Boids is an autonomous world — ORCHESTRATE commands are not supported.
   * Returns a failure for all actions.
   */
  @Override
  public WorldResult orchestrate(OrchestrateRequest req, SimulationGridState state) {
    return WorldResult.failed("orchestrator", state.worldName(),
        "Boids world does not support ORCHESTRATE commands.");
  }

  // ── Internal ───────────────────────────────────────────────────────────────

  private void registerSystems() {
    // Stores must be registered before systems that read them.
    ecsWorld.denseStore(PositionComp.class);
    ecsWorld.denseStore(VelocityComp.class);
    ecsWorld.denseStore(BoidComp.class);

    ecsWorld.addSystem(BoidsSystems.flockingSystem(spatialHash));
    ecsWorld.addSystem(BoidsSystems.movementSystem());
    ecsWorld.addSystem(BoidsSystems.boundsSystem(worldW, worldH));
  }

  private void spawnBoids(int count) {
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    ComponentStore<PositionComp> positions = ecsWorld.denseStore(PositionComp.class);
    ComponentStore<VelocityComp> velocities = ecsWorld.denseStore(VelocityComp.class);
    ComponentStore<BoidComp> boidStore = ecsWorld.denseStore(BoidComp.class);

    for (int i = 0; i < count; i++) {
      EntityId id = ecsWorld.create();
      float x = (float) (rng.nextDouble() * worldW);
      float y = (float) (rng.nextDouble() * worldH);

      // Random unit-vector velocity scaled to SPAWN_SPEED
      double angle = rng.nextDouble() * 2 * Math.PI;
      float vx = (float) (Math.cos(angle) * SPAWN_SPEED);
      float vy = (float) (Math.sin(angle) * SPAWN_SPEED);

      positions.set(id, new PositionComp(x, y));
      velocities.set(id, new VelocityComp(vx, vy));
      boidStore.set(id, BoidComp.defaults());
    }
  }

  private void rebuildHash() {
    spatialHash.clear();
    ComponentStore<PositionComp> positions = ecsWorld.store(PositionComp.class);
    if (positions != null) {
      positions.forEach((id, pos) -> spatialHash.insert(id, pos.x(), pos.y()));
    }
  }

  private GridSnapshot buildSnapshot() {
    ComponentStore<PositionComp> positions = ecsWorld.store(PositionComp.class);
    if (positions == null) {
      return new GridSnapshot(Map.of(), Map.of());
    }
    Map<String, EntityView> entities = new HashMap<>(initialCount);
    positions.forEach((id, pos) ->
        entities.put("boid-" + id.id(), new EntityView((int) pos.x(), (int) pos.y()))
    );
    return new GridSnapshot(Map.of(), entities);
  }

  private String buildJson(boolean oneLine) {
    String sep = oneLine ? "" : "\n";
    String indent = oneLine ? "" : "  ";
    StringBuilder sb = new StringBuilder("{").append(sep);
    sb.append(indent).append("\"type\": \"full\",").append(sep);
    sb.append(indent).append("\"tiles\": {},").append(sep);
    sb.append(indent).append("\"entities\": {");

    ComponentStore<PositionComp> positions = ecsWorld.store(PositionComp.class);
    boolean first = true;
    if (positions != null) {
      for (int i = 0; i < ecsWorld.entityCount(); i++) {
        EntityId id = new EntityId(i);
        PositionComp pos = positions.get(id);
        if (pos == null) {
          continue;
        }
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append(sep).append(indent).append(indent)
            .append("\"boid-").append(i).append("\":{\"x\":").append((int) pos.x())
            .append(",\"y\":").append((int) pos.y()).append("}");
      }
    }

    sb.append(sep).append(indent).append("}").append(sep).append("}");
    return sb.toString();
  }
}
