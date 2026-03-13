package com.ntros.model.world.wator;

/**
 * Tags an entity slot in {@link WaTorWorldState} with its logical role in the simulation.
 * Used by systems that need to distinguish creatures from passive entities, and by the
 * serialiser to produce correctly typed snapshot frames for the client.
 */
public enum EntityType {
  PREDATOR,
  PREY,
  PLANT,
  FOOD
}
