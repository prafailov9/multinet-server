package com.ntros.model.world.wator.system;

import com.ntros.model.world.wator.WaTorWorldState;
import com.ntros.model.world.wator.component.EnergyComponent;
import com.ntros.model.world.wator.component.GenomeComponent;
import com.ntros.model.world.wator.component.HealthComponent;
import com.ntros.model.world.wator.component.VelocityComponent;
import com.ntros.model.world.wator.component.VisionComponent;
import com.ntros.model.world.wator.neural.Brain;

/**
 * Evaluates each creature's neural network and writes motor commands back into its
 * {@link VelocityComponent}.
 *
 * <p>Must run <em>after</em> {@link VisionSystem} (so {@link VisionComponent} is populated)
 * and <em>before</em> {@link MovementSystem} (so the updated velocity is integrated).
 */
public final class BrainSystem implements WaTorSystem {

  @Override
  public void tick(WaTorWorldState state, float dt) {
    int size = state.positions.size();
    for (int id = 0; id < size; id++) {
      if (!state.alive.get(id)) continue;

      GenomeComponent  genome  = state.genomes.get(id);
      VisionComponent  vision  = state.visions.get(id);
      VelocityComponent vel    = state.velocities.get(id);
      EnergyComponent  energy  = state.energies.get(id);
      HealthComponent  health  = state.healths.get(id);

      if (genome == null || vision == null || vel == null) continue;  // not a creature

      float normEnergy = (energy != null) ? energy.current / energy.max : 0f;
      float normHealth = (health != null) ? health.current / health.max : 0f;

      Brain.evaluate(genome, vision, normEnergy, normHealth, vel);
    }
  }
}
