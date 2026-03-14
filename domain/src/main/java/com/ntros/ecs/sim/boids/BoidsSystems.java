package com.ntros.ecs.sim.boids;

import com.ntros.ecs.components.BoidComp;
import com.ntros.ecs.components.PositionComp;
import com.ntros.ecs.components.VelocityComp;
import com.ntros.ecs.core.ComponentStore;
import com.ntros.ecs.core.EcsSystem;
import com.ntros.ecs.core.EcsWorld;
import com.ntros.ecs.core.EntityId;
import java.util.List;

/**
 * ECS systems implementing Reynolds' boids flocking algorithm.
 *
 * <h3>System pipeline (registration order)</h3>
 * <ol>
 *   <li>{@link #flockingSystem} — reads positions from the pre-built spatial hash;
 *       applies separation, alignment, and cohesion forces to velocities.</li>
 *   <li>{@link #movementSystem} — integrates velocity into position (Euler step).</li>
 *   <li>{@link #boundsSystem} — wraps boids that leave world boundaries (toroidal).</li>
 * </ol>
 *
 * <p>The spatial hash is rebuilt each tick by the {@link BoidsEngine} before systems run,
 * so all three rules read consistent start-of-tick positions.
 *
 * <h3>Steering parameters</h3>
 * <ul>
 *   <li>{@code MAX_SPEED} — terminal speed in world units per second.</li>
 *   <li>{@code MAX_FORCE} — maximum steering acceleration per second.</li>
 *   <li>{@code SEP_WEIGHT / ALIGN_WEIGHT / COH_WEIGHT} — rule contribution ratios.</li>
 * </ul>
 */
public final class BoidsSystems {

  private static final float MAX_SPEED = 25f;   // world-units / second
  private static final float MAX_FORCE = 60f;   // world-units / second² (steering acceleration)

  private static final float SEP_WEIGHT = 1.8f;
  private static final float ALIGN_WEIGHT = 1.0f;
  private static final float COH_WEIGHT = 0.9f;

  private BoidsSystems() {
  }

  // ── Public factory methods ─────────────────────────────────────────────────

  /**
   * Returns a system that applies separation, alignment, and cohesion to every boid's velocity.
   *
   * <p>A single pass per boid gathers all three rules' contributions from the same neighbourhood
   * scan — one spatial-hash lookup per boid instead of three.
   *
   * @param hash the spatial hash rebuilt by the engine at the start of each tick
   */
  public static EcsSystem flockingSystem(BoidSpatialHash hash) {
    return (world, dt) -> {
      ComponentStore<PositionComp> positions = world.store(PositionComp.class);
      ComponentStore<VelocityComp> velocities = world.store(VelocityComp.class);
      ComponentStore<BoidComp> boids = world.store(BoidComp.class);
      if (positions == null || velocities == null || boids == null) {
        return;
      }

      positions.forEach((id, pos) -> {
        BoidComp boid = boids.get(id);
        VelocityComp vel = velocities.get(id);
        if (boid == null || vel == null) {
          return;
        }

        // Gather neighbours within the largest radius to avoid duplicate lookups.
        float maxR = Math.max(boid.sepRadius(), Math.max(boid.alignRadius(), boid.cohRadius()));
        List<EntityId> candidates = hash.nearby(pos.x(), pos.y(), maxR);

        float sepX = 0, sepY = 0;
        float alignVx = 0, alignVy = 0;
        float cohX = 0, cohY = 0;
        int sepCount = 0, alignCount = 0, cohCount = 0;

        for (EntityId other : candidates) {
          if (other.equals(id)) {
            continue;
          }
          PositionComp op = positions.get(other);
          VelocityComp ov = velocities.get(other);
          if (op == null) {
            continue;
          }
          float dx = pos.x() - op.x();
          float dy = pos.y() - op.y();
          float dist2 = dx * dx + dy * dy;
          float dist = (float) Math.sqrt(dist2);

          // ── Separation ────────────────────────────────────────────────────
          if (dist < boid.sepRadius() && dist > 0f) {
            sepX += dx / dist;
            sepY += dy / dist;
            sepCount++;
          }

          // ── Alignment ─────────────────────────────────────────────────────
          if (dist < boid.alignRadius() && ov != null) {
            alignVx += ov.vx();
            alignVy += ov.vy();
            alignCount++;
          }

          // ── Cohesion ──────────────────────────────────────────────────────
          if (dist < boid.cohRadius()) {
            cohX += op.x();
            cohY += op.y();
            cohCount++;
          }
        }

        float steerX = 0, steerY = 0;

        if (sepCount > 0) {
          float[] sep = steer(vel, normalize(sepX, sepY, MAX_SPEED), MAX_FORCE);
          steerX += sep[0] * SEP_WEIGHT;
          steerY += sep[1] * SEP_WEIGHT;
        }
        if (alignCount > 0) {
          float[] align = steer(vel,
              normalize(alignVx / alignCount, alignVy / alignCount, MAX_SPEED), MAX_FORCE);
          steerX += align[0] * ALIGN_WEIGHT;
          steerY += align[1] * ALIGN_WEIGHT;
        }
        if (cohCount > 0) {
          float targetX = cohX / cohCount - pos.x();
          float targetY = cohY / cohCount - pos.y();
          float[] coh = steer(vel, normalize(targetX, targetY, MAX_SPEED), MAX_FORCE);
          steerX += coh[0] * COH_WEIGHT;
          steerY += coh[1] * COH_WEIGHT;
        }

        if (steerX != 0f || steerY != 0f) {
          float nvx = vel.vx() + steerX * dt;
          float nvy = vel.vy() + steerY * dt;
          // Clamp to MAX_SPEED
          float[] clamped = clamp(nvx, nvy, MAX_SPEED);
          velocities.set(id, new VelocityComp(clamped[0], clamped[1]));
        }
      });
    };
  }

  /**
   * Returns a system that moves each boid by {@code velocity × dt} (Euler integration).
   */
  public static EcsSystem movementSystem() {
    return (world, dt) -> {
      ComponentStore<PositionComp> positions = world.store(PositionComp.class);
      ComponentStore<VelocityComp> velocities = world.store(VelocityComp.class);
      if (positions == null || velocities == null) {
        return;
      }
      world.query(PositionComp.class, VelocityComp.class, (id, pos, vel) ->
          positions.set(id, new PositionComp(pos.x() + vel.vx() * dt, pos.y() + vel.vy() * dt))
      );
    };
  }

  /**
   * Returns a system that wraps boids toroidally when they cross world boundaries.
   *
   * @param worldW world width (exclusive upper bound for x)
   * @param worldH world height (exclusive upper bound for y)
   */
  public static EcsSystem boundsSystem(float worldW, float worldH) {
    return (world, dt) -> {
      ComponentStore<PositionComp> positions = world.store(PositionComp.class);
      if (positions == null) {
        return;
      }
      positions.forEach((id, pos) -> {
        float x = pos.x();
        float y = pos.y();
        boolean changed = false;
        if (x < 0f) {
          x += worldW;
          changed = true;
        } else if (x >= worldW) {
          x -= worldW;
          changed = true;
        }
        if (y < 0f) {
          y += worldH;
          changed = true;
        } else if (y >= worldH) {
          y -= worldH;
          changed = true;
        }
        if (changed) {
          positions.set(id, new PositionComp(x, y));
        }
      });
    };
  }

  // ── Private steering helpers ───────────────────────────────────────────────

  /**
   * Scales {@code (x, y)} to length {@code targetSpeed}, returning the desired velocity vector.
   * Returns the zero vector if the input has zero length.
   */
  private static float[] normalize(float x, float y, float targetSpeed) {
    float len = (float) Math.sqrt(x * x + y * y);
    if (len < 1e-6f) {
      return new float[]{0f, 0f};
    }
    return new float[]{x / len * targetSpeed, y / len * targetSpeed};
  }

  /**
   * Computes the Reynolds steering vector: {@code desired - current}, clamped to
   * {@code maxForce}.
   */
  private static float[] steer(VelocityComp vel, float[] desired, float maxForce) {
    float sx = desired[0] - vel.vx();
    float sy = desired[1] - vel.vy();
    return clamp(sx, sy, maxForce);
  }

  /** Clamps vector {@code (x, y)} to at most {@code max} length. */
  private static float[] clamp(float x, float y, float max) {
    float len = (float) Math.sqrt(x * x + y * y);
    if (len > max) {
      float scale = max / len;
      return new float[]{x * scale, y * scale};
    }
    return new float[]{x, y};
  }
}
