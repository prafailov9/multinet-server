package com.ntros.model.world.wator;

import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;

/**
 * Engine contract for the Wa-Tor continuous-space predator-prey simulation.
 *
 * <p>This is a deliberately thin interface — the ECS {@link WaTorWorld} owns all simulation
 * logic.  The engine's role is to bridge the server's {@link com.ntros.model.world.connector.WorldConnector}
 * lifecycle (join, tick, snapshot, reset) to the world ECS container.
 *
 * <p>Mirrors the role of {@link com.ntros.model.world.engine.core.GridEngine} for grid worlds
 * and {@link com.ntros.model.world.engine.core.DynamicWorldEngine} for the 3D open world,
 * without sharing any type hierarchy with either — the {@link com.ntros.model.world.connector.WaTorConnector}
 * facade is the only integration point visible to the server infrastructure.
 */
public interface WaTorEngine {

  /**
   * Advances the simulation by one tick.
   *
   * @param world mutable ECS container
   * @param dt    elapsed time in seconds
   */
  void tick(WaTorWorld world, float dt);

  /**
   * Registers a new observer session (spectator join — agents are autonomous).
   *
   * @param req   join request carrying the observer's name
   * @param world ECS container
   * @return success result with the observer's assigned ID
   */
  WorldResult joinObserver(JoinRequest req, WaTorWorld world);

  /**
   * Removes an observer from the registry.
   *
   * @param observerName name used during join
   * @param world        ECS container
   */
  void removeObserver(String observerName, WaTorWorld world);

  /**
   * Serialises the current world state for broadcast.
   *
   * @param world ECS container
   * @return snapshot POJO — serialised to JSON by the broadcast layer
   */
  Object snapshot(WaTorWorld world);

  /**
   * Resets the simulation to its initial state.
   *
   * @param world ECS container to reset
   */
  void reset(WaTorWorld world);
}
