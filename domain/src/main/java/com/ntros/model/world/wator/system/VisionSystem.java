package com.ntros.model.world.wator.system;

import com.ntros.model.world.wator.EntityType;
import com.ntros.model.world.wator.WaTorWorldState;
import com.ntros.model.world.wator.component.Position2f;
import com.ntros.model.world.wator.component.VisionComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Populates each creature's {@link VisionComponent} from the current spatial hash.
 *
 * <h3>Algorithm (per creature)</h3>
 * <ol>
 *   <li>Reset all zone readings to empty.</li>
 *   <li>Query the spatial hash for entities within {@link VisionComponent#rayLength}.</li>
 *   <li>For each candidate, compute distance and angle relative to the creature's heading.</li>
 *   <li>Map the relative angle to one of the 5 zones.</li>
 *   <li>If closer than the zone's current best, overwrite the zone's type and distance values.</li>
 * </ol>
 *
 * <h3>Entity type encoding</h3>
 * <pre>
 *   empty     → 0.00
 *   food      → 0.25
 *   plant     → 0.50
 *   prey      → 0.75
 *   predator  → 1.00
 * </pre>
 */
public final class VisionSystem implements WaTorSystem {

  private static final float TYPE_EMPTY    = 0.00f;
  private static final float TYPE_FOOD     = 0.25f;
  private static final float TYPE_PLANT    = 0.50f;
  private static final float TYPE_PREY     = 0.75f;
  private static final float TYPE_PREDATOR = 1.00f;

  private final List<Integer> candidates = new ArrayList<>(64);

  @Override
  public void tick(WaTorWorldState state, float dt) {
    int size = state.positions.size();
    for (int id = 0; id < size; id++) {
      if (!state.alive.get(id)) continue;

      VisionComponent vision = state.visions.get(id);
      if (vision == null) continue;   // only creatures have VisionComponent

      Position2f selfPos = state.positions.get(id);
      float selfAngle = state.velocities.get(id).angle;

      vision.reset();

      // Gather nearby entity IDs from spatial hash
      candidates.clear();
      state.spatialHash.queryRadius(selfPos.x, selfPos.y, vision.rayLength, candidates);

      float halfFov  = vision.fieldOfView * 0.5f;
      float zoneArc  = vision.fieldOfView / VisionComponent.ZONE_COUNT;

      for (int candidateId : candidates) {
        if (candidateId == id) continue;                // skip self
        if (!state.alive.get(candidateId)) continue;

        Position2f otherPos = state.positions.get(candidateId);
        if (otherPos == null) continue;

        float dx = otherPos.x - selfPos.x;
        float dy = otherPos.y - selfPos.y;
        float dist2 = dx * dx + dy * dy;
        float maxR  = vision.rayLength;
        if (dist2 > maxR * maxR) continue;    // outside circle (bucket may over-include)

        float dist = (float) Math.sqrt(dist2);
        float normDist = dist / maxR;

        // Angle from self to candidate, relative to self's heading
        float absAngle     = (float) Math.atan2(dy, dx);
        float relAngle     = wrapAngle(absAngle - selfAngle);

        // Is it within FOV?
        if (relAngle < -halfFov || relAngle > halfFov) continue;

        // Map to zone index (0 = far-left, 4 = far-right)
        int zoneIdx = (int) ((relAngle + halfFov) / zoneArc);
        zoneIdx = Math.min(zoneIdx, VisionComponent.ZONE_COUNT - 1);

        // Keep closest hit per zone
        if (normDist < vision.zoneDist[zoneIdx]) {
          vision.zoneType[zoneIdx] = encodeType(state.types.get(candidateId));
          vision.zoneDist[zoneIdx] = normDist;
        }
      }
    }
  }

  private static float encodeType(EntityType type) {
    return switch (type) {
      case FOOD     -> TYPE_FOOD;
      case PLANT    -> TYPE_PLANT;
      case PREY     -> TYPE_PREY;
      case PREDATOR -> TYPE_PREDATOR;
    };
  }

  /** Wraps an angle into [-π, π]. */
  private static float wrapAngle(float a) {
    while (a >  Math.PI) a -= (float) (2 * Math.PI);
    while (a < -Math.PI) a += (float) (2 * Math.PI);
    return a;
  }
}
