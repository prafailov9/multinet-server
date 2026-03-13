package com.ntros.model.world.wator.system;

import com.ntros.model.world.wator.WaTorWorldState;
import com.ntros.model.world.wator.component.Position2f;
import com.ntros.model.world.wator.component.VelocityComponent;

/**
 * Integrates creature velocity into position using Euler integration.
 *
 * <h3>Per tick</h3>
 * <ol>
 *   <li>Apply {@link VelocityComponent#angularVel} to update the heading angle.</li>
 *   <li>Clamp {@link VelocityComponent#speed} to [0, MAX_SPEED].</li>
 *   <li>Convert polar (angle, speed) to Cartesian (dx, dy) and add to position.</li>
 *   <li>Wrap position to world bounds (toroidal — creatures emerge on the opposite edge).</li>
 * </ol>
 *
 * <p>Must run <em>after</em> {@link BrainSystem}.
 */
public final class MovementSystem implements WaTorSystem {

  @Override
  public void tick(WaTorWorldState state, float dt) {
    int size = state.positions.size();
    for (int id = 0; id < size; id++) {
      if (!state.alive.get(id)) continue;

      Position2f        pos = state.positions.get(id);
      VelocityComponent vel = state.velocities.get(id);

      if (pos == null || vel == null) continue;  // plants / food have no velocity

      // 1. Update heading
      vel.angle += vel.angularVel * dt;
      // Keep angle in [-π, π] to avoid floating-point drift
      if      (vel.angle >  Math.PI) vel.angle -= (float) (2 * Math.PI);
      else if (vel.angle < -Math.PI) vel.angle += (float) (2 * Math.PI);

      // 2. Clamp speed
      vel.speed = Math.max(0f, Math.min(vel.speed, VelocityComponent.MAX_SPEED));

      // 3. Integrate position
      float dx = (float) Math.cos(vel.angle) * vel.speed * dt;
      float dy = (float) Math.sin(vel.angle) * vel.speed * dt;
      pos.x += dx;
      pos.y += dy;

      // 4. Toroidal wrap
      pos.x = wrap(pos.x, WaTorWorldState.WIDTH);
      pos.y = wrap(pos.y, WaTorWorldState.HEIGHT);
    }
  }

  private static float wrap(float v, float max) {
    if (v < 0f)   return v + max;
    if (v >= max) return v - max;
    return v;
  }
}
