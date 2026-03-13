package com.ntros.model.world.wator;

import com.ntros.model.world.wator.component.GenomeComponent;

/**
 * Deferred entity-creation request queued during a tick (e.g. food-on-death, reproduction).
 * Processed by {@link WaTorWorld} after all systems have finished, so that component-array
 * iteration in the systems is never invalidated by mid-tick structural changes.
 *
 * @param type         The type of the entity to spawn.
 * @param x            Spawn X coordinate (world units).
 * @param y            Spawn Y coordinate (world units).
 * @param genome       Genome to assign (non-null for PREDATOR / PREY, null otherwise).
 * @param energyValue  Initial energy (for PREDATOR/PREY) or nutrition value (for FOOD).
 */
public record SpawnRequest(
    EntityType type,
    float x,
    float y,
    GenomeComponent genome,
    float energyValue
) {

  /** Convenience factory for FOOD spawns. */
  public static SpawnRequest food(float x, float y, float nutrition) {
    return new SpawnRequest(EntityType.FOOD, x, y, null, nutrition);
  }

  /** Convenience factory for offspring spawns (PREDATOR or PREY). */
  public static SpawnRequest offspring(EntityType type, float x, float y,
      GenomeComponent genome, float initialEnergy) {
    return new SpawnRequest(type, x, y, genome, initialEnergy);
  }
}
