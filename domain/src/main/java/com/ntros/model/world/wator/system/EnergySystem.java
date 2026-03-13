package com.ntros.model.world.wator.system;

import com.ntros.model.world.wator.EntityType;
import com.ntros.model.world.wator.SpawnRequest;
import com.ntros.model.world.wator.WaTorWorldState;
import com.ntros.model.world.wator.component.EnergyComponent;
import com.ntros.model.world.wator.component.HealthComponent;
import com.ntros.model.world.wator.component.Position2f;
import com.ntros.model.world.wator.component.VelocityComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Drains creature energy from movement and idle metabolism, then decays health
 * when a creature is starving. Also handles eating interactions (predator eats prey /
 * food; prey eats plants).
 *
 * <h3>Energy drain per tick</h3>
 * <pre>
 *   total_drain = speed² × MOVEMENT_COST_FACTOR + IDLE_DRAIN
 * </pre>
 *
 * <h3>Eating (interaction range = INTERACTION_RADIUS)</h3>
 * A creature that overlaps an edible entity this tick consumes it:
 * <ul>
 *   <li>Predator eats Food  → gains food's nutrition value</li>
 *   <li>Predator attacks Prey → deals PREDATOR_ATTACK_DAMAGE to prey HP; prey deals
 *       PREY_DEFENCE_DAMAGE back; if prey dies, predator gains PREY_NUTRITION_FRACTION
 *       of prey's remaining energy and prey leaves food behind</li>
 *   <li>Prey eats Plant → gains PLANT_NUTRITION; plant entity is removed</li>
 * </ul>
 *
 * <p>Must run <em>after</em> {@link MovementSystem} (so positions are up-to-date)
 * and <em>before</em> {@link DeathSystem} (so dead entities are still present for food-drop).
 */
public final class EnergySystem implements WaTorSystem {

  private final List<Integer> nearby = new ArrayList<>(32);

  @Override
  public void tick(WaTorWorldState state, float dt) {
    int size = state.positions.size();

    for (int id = 0; id < size; id++) {
      if (!state.alive.get(id)) continue;

      EnergyComponent energy = state.energies.get(id);
      if (energy == null) continue;  // plants have no energy

      EntityType type = state.types.get(id);
      if (type == EntityType.FOOD) continue;  // food doesn't metabolise

      // ── Step 1: drain energy ──────────────────────────────────────────
      VelocityComponent vel = state.velocities.get(id);
      float speed = (vel != null) ? vel.speed : 0f;
      float drain = speed * speed * EnergyComponent.MOVEMENT_COST_FACTOR * dt
          + EnergyComponent.IDLE_DRAIN * dt;
      energy.drain(drain);

      // ── Step 2: starving → health decay ──────────────────────────────
      if (energy.isDepleted()) {
        HealthComponent hp = state.healths.get(id);
        if (hp != null) {
          hp.damage(HealthComponent.STARVATION_DECAY * dt);
        }
      }

      // ── Step 3: eating interactions ───────────────────────────────────
      Position2f pos = state.positions.get(id);
      nearby.clear();
      state.spatialHash.queryRadius(pos.x, pos.y, HealthComponent.INTERACTION_RADIUS, nearby);

      for (int otherId : nearby) {
        if (otherId == id) continue;
        if (!state.alive.get(otherId)) continue;

        EntityType otherType = state.types.get(otherId);
        Position2f otherPos  = state.positions.get(otherId);
        if (otherPos == null) continue;

        float dx = otherPos.x - pos.x;
        float dy = otherPos.y - pos.y;
        if (dx * dx + dy * dy > HealthComponent.INTERACTION_RADIUS * HealthComponent.INTERACTION_RADIUS) {
          continue;
        }

        if (type == EntityType.PREDATOR) {
          handlePredatorInteraction(id, otherId, otherType, state);
        } else if (type == EntityType.PREY && otherType == EntityType.PLANT) {
          handlePreyEatsPlant(id, otherId, state);
        }
      }
    }
  }

  private void handlePredatorInteraction(int predId, int otherId,
      EntityType otherType, WaTorWorldState state) {

    EnergyComponent predEnergy = state.energies.get(predId);

    if (otherType == EntityType.FOOD) {
      // Predator eats food — gain nutrition (energy.max = nutrition; energy.current = TTL)
      EnergyComponent foodEnergy = state.energies.get(otherId);
      if (foodEnergy != null) {
        predEnergy.replenish(foodEnergy.max);
      }
      state.alive.set(otherId, false);
      state.pendingRemovals.add(otherId);

    } else if (otherType == EntityType.PREY) {
      // Predator attacks prey
      HealthComponent preyHp = state.healths.get(otherId);
      HealthComponent predHp = state.healths.get(predId);

      if (preyHp != null) {
        preyHp.damage(HealthComponent.PREDATOR_ATTACK_DAMAGE);
      }
      if (predHp != null) {
        // Prey fights back
        predHp.damage(HealthComponent.PREY_DEFENCE_DAMAGE);
      }
      // If prey is now dead, predator gains energy (DeathSystem drops food separately)
      if (preyHp != null && preyHp.isDead()) {
        EnergyComponent preyEnergy = state.energies.get(otherId);
        if (preyEnergy != null) {
          predEnergy.replenish(preyEnergy.current * EnergyComponent.PREY_NUTRITION_FRACTION);
        }
      }
    }
  }

  private void handlePreyEatsPlant(int preyId, int plantId, WaTorWorldState state) {
    EnergyComponent preyEnergy = state.energies.get(preyId);
    if (preyEnergy != null) {
      preyEnergy.replenish(EnergyComponent.PLANT_NUTRITION);
    }
    state.alive.set(plantId, false);
    state.pendingRemovals.add(plantId);
  }
}
