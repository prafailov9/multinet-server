package com.ntros.model.world.wator.system;

import com.ntros.model.world.wator.EntityType;
import com.ntros.model.world.wator.SpawnRequest;
import com.ntros.model.world.wator.WaTorWorldState;
import com.ntros.model.world.wator.component.AgentComponent;
import com.ntros.model.world.wator.component.EnergyComponent;
import com.ntros.model.world.wator.component.GenomeComponent;
import com.ntros.model.world.wator.component.Position2f;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tracks reproduction progress for creatures and queues offspring spawns.
 *
 * <h3>Reproduction logic</h3>
 * Each tick, if a creature's current energy exceeds
 * {@link AgentComponent#SURPLUS_ENERGY_FRACTION} × max energy, its
 * {@link AgentComponent#reproductionProgress} counter increments by 1.
 * When the counter reaches {@link AgentComponent#REPRODUCTION_THRESHOLD}:
 * <ul>
 *   <li>An offspring is queued at a small random offset from the parent's position.</li>
 *   <li>The offspring's genome is the parent's genome, mutated.</li>
 *   <li>The parent's energy is halved (cost of reproduction).</li>
 *   <li>The reproduction counter resets to 0.</li>
 * </ul>
 */
public final class ReproductionSystem implements WaTorSystem {

  /** Max spawn offset from parent (world units). */
  private static final float OFFSPRING_OFFSET = 10f;

  @Override
  public void tick(WaTorWorldState state, float dt) {
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    int size = state.positions.size();

    for (int id = 0; id < size; id++) {
      if (!state.alive.get(id)) continue;

      AgentComponent  agent  = state.agents.get(id);
      EnergyComponent energy = state.energies.get(id);
      GenomeComponent genome = state.genomes.get(id);
      if (agent == null || energy == null || genome == null) continue;

      // Accrue reproduction progress only when well-fed
      if (energy.current >= AgentComponent.SURPLUS_ENERGY_FRACTION * energy.max) {
        agent.reproductionProgress += 1f;
      } else if (agent.reproductionProgress > 0f) {
        // Regress slowly when hungry
        agent.reproductionProgress = Math.max(0f, agent.reproductionProgress - 0.5f);
      }

      if (agent.reproductionProgress >= AgentComponent.REPRODUCTION_THRESHOLD) {
        agent.reproductionProgress = 0f;

        // Halve parent energy
        energy.current *= 0.5f;

        // Spawn offspring near parent
        Position2f pos = state.positions.get(id);
        double angle   = rng.nextDouble() * 2 * Math.PI;
        float ox = wrap(pos.x + (float) (Math.cos(angle) * OFFSPRING_OFFSET),
            WaTorWorldState.WIDTH);
        float oy = wrap(pos.y + (float) (Math.sin(angle) * OFFSPRING_OFFSET),
            WaTorWorldState.HEIGHT);

        EntityType type = state.types.get(id);
        state.pendingSpawns.add(SpawnRequest.offspring(
            type, ox, oy,
            genome.mutate(),
            energy.max * 0.5f   // offspring starts at half max energy
        ));
      }
    }
  }

  private static float wrap(float v, float max) {
    if (v < 0f)   return v + max;
    if (v >= max) return v - max;
    return v;
  }
}
