package com.ntros.model.world.wator.system;

import com.ntros.model.world.wator.EntityType;
import com.ntros.model.world.wator.SpawnRequest;
import com.ntros.model.world.wator.WaTorWorldState;
import com.ntros.model.world.wator.component.EnergyComponent;
import com.ntros.model.world.wator.component.HealthComponent;
import com.ntros.model.world.wator.component.Position2f;

/**
 * Marks dead creatures for removal and queues food-on-death spawns.
 *
 * <h3>Death conditions</h3>
 * An entity dies when {@link HealthComponent#isDead()} returns true.
 * Starvation is handled upstream by {@link EnergySystem}, which decays HP whenever
 * energy is depleted.
 *
 * <h3>Food-on-death</h3>
 * When a PREDATOR or PREY dies, a {@link EntityType#FOOD} spawn is queued at the same
 * position.  Only predators consume food (enforced in {@link EnergySystem}).
 *
 * <h3>Food component layout (repurposed {@link EnergyComponent} fields)</h3>
 * <ul>
 *   <li>{@link EnergyComponent#current} — TTL countdown (ticks remaining before despawn).</li>
 *   <li>{@link EnergyComponent#max}     — nutrition value granted to the predator that eats it.</li>
 * </ul>
 * {@link EnergySystem} reads {@code energy.max} as nutrition; this system decrements
 * {@code energy.current} as the TTL countdown.
 */
public final class DeathSystem implements WaTorSystem {

  /** Fraction of a dead creature's energy that becomes food nutrition. */
  private static final float FOOD_ENERGY_FRACTION = 0.5f;

  /** Ticks food persists before despawning if uneaten. */
  public static final float FOOD_TTL_TICKS = 400f;

  @Override
  public void tick(WaTorWorldState state, float dt) {
    int size = state.positions.size();

    for (int id = 0; id < size; id++) {
      if (!state.alive.get(id)) continue;

      EntityType type = state.types.get(id);

      // ── Food: decrement TTL countdown ─────────────────────────────────
      if (type == EntityType.FOOD) {
        EnergyComponent e = state.energies.get(id);
        if (e != null) {
          e.current -= 1f;
          if (e.current <= 0f) {
            markDead(state, id);
          }
        }
        continue;
      }

      // ── Creatures: check HP ───────────────────────────────────────────
      HealthComponent hp = state.healths.get(id);
      if (hp == null || !hp.isDead()) continue;

      markDead(state, id);

      // Drop food at death location (predators and prey only)
      if (type == EntityType.PREDATOR || type == EntityType.PREY) {
        Position2f pos     = state.positions.get(id);
        EnergyComponent en = state.energies.get(id);
        float nutrition    = (en != null) ? en.current * FOOD_ENERGY_FRACTION : 5f;

        // SpawnRequest.energyValue = nutrition; WaTorWorld sets:
        //   EnergyComponent(current=FOOD_TTL_TICKS, max=nutrition)
        state.pendingSpawns.add(SpawnRequest.food(pos.x, pos.y, nutrition));
      }
    }
  }

  private static void markDead(WaTorWorldState state, int id) {
    state.alive.set(id, false);
    state.pendingRemovals.add(id);
  }
}
