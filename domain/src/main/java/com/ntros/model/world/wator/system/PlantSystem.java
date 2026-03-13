package com.ntros.model.world.wator.system;

import com.ntros.model.world.wator.EntityType;
import com.ntros.model.world.wator.SpawnRequest;
import com.ntros.model.world.wator.WaTorWorldState;
import com.ntros.model.world.wator.component.PlantComponent;
import com.ntros.model.world.wator.component.Position2f;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Grows existing plants and handles both directed spread and spontaneous emergence.
 *
 * <h3>Per tick, for each alive plant</h3>
 * <ol>
 *   <li>Increment {@link PlantComponent#size} by {@link PlantComponent#GROWTH_RATE}.</li>
 *   <li>If fully grown and the spread cooldown has elapsed, attempt to place a new plant
 *       within {@link PlantComponent#SPREAD_RADIUS} of the parent at a location not
 *       already occupied by another plant.</li>
 * </ol>
 *
 * <h3>Spontaneous emergence</h3>
 * Each tick there is a {@link PlantComponent#SPONTANEOUS_SPAWN_CHANCE} probability that
 * a new plant appears at a random location in the world.
 */
public final class PlantSystem implements WaTorSystem {

  private final List<Integer> nearby = new ArrayList<>(16);

  @Override
  public void tick(WaTorWorldState state, float dt) {
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    int size = state.positions.size();

    for (int id = 0; id < size; id++) {
      if (!state.alive.get(id)) continue;

      PlantComponent plant = state.plants.get(id);
      if (plant == null) continue;

      // ── Grow ─────────────────────────────────────────────────────────
      if (plant.size < PlantComponent.MAX_SIZE) {
        plant.size = Math.min(PlantComponent.MAX_SIZE,
            plant.size + PlantComponent.GROWTH_RATE);
      }

      // ── Spread ───────────────────────────────────────────────────────
      if (plant.isFullyGrown()) {
        plant.ticksSinceLastSpread++;
        if (plant.ticksSinceLastSpread >= PlantComponent.SPREAD_COOLDOWN_TICKS) {
          plant.ticksSinceLastSpread = 0;
          trySpread(id, state, rng);
        }
      }
    }

    // ── Spontaneous emergence ─────────────────────────────────────────
    if (rng.nextDouble() < PlantComponent.SPONTANEOUS_SPAWN_CHANCE) {
      float x = rng.nextFloat() * WaTorWorldState.WIDTH;
      float y = rng.nextFloat() * WaTorWorldState.HEIGHT;
      state.pendingSpawns.add(new SpawnRequest(EntityType.PLANT, x, y, null, 0f));
    }
  }

  private void trySpread(int parentId, WaTorWorldState state, ThreadLocalRandom rng) {
    Position2f parentPos = state.positions.get(parentId);

    // Check whether there is already a plant in the spread neighbourhood
    nearby.clear();
    state.spatialHash.queryRadius(
        parentPos.x, parentPos.y, PlantComponent.SPREAD_RADIUS, nearby);

    int plantCount = 0;
    for (int nid : nearby) {
      if (state.alive.get(nid) && state.plants.get(nid) != null) {
        plantCount++;
      }
    }

    // Don't spread into a dense cluster (cap at 4 neighbours)
    if (plantCount >= 4) return;

    // Pick a random position within spread radius
    double angle = rng.nextDouble() * 2 * Math.PI;
    double dist  = rng.nextDouble() * PlantComponent.SPREAD_RADIUS;
    float sx = (float) (parentPos.x + Math.cos(angle) * dist);
    float sy = (float) (parentPos.y + Math.sin(angle) * dist);

    // Clamp to world bounds
    sx = Math.max(0f, Math.min(WaTorWorldState.WIDTH  - 1f, sx));
    sy = Math.max(0f, Math.min(WaTorWorldState.HEIGHT - 1f, sy));

    state.pendingSpawns.add(new SpawnRequest(EntityType.PLANT, sx, sy, null, 0f));
  }
}
