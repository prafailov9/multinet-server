package com.ntros.model.world.engine.open;

import static com.ntros.model.world.protocol.WorldResult.failed;
import static com.ntros.model.world.protocol.WorldResult.succeeded;

import com.ntros.model.entity.movement.vectors.Vector3;
import com.ntros.model.entity.movement.velocity.Velocity3;
import com.ntros.model.entity.open.OpenWorldEntity;
import com.ntros.model.entity.open.OpenWorldPlayer;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.OpenMoveRequest;
import com.ntros.model.world.state.dimension.Dimension3D;
import com.ntros.model.world.state.open.DynamicWorldState;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * Physics-driven engine for the continuous 3D open world.
 *
 * <h3>Tick lifecycle</h3>
 * <ol>
 *   <li>{@link #storeMoveIntent} — queues a normalised thrust direction per entity (called from
 *       the session virtual thread via the actor's {@code stageMove} path).</li>
 *   <li>{@link #applyIntents} — drains intents, accelerates entities, clamps speed,
 *       integrates positions, enforces boundary, and clears intents.</li>
 * </ol>
 *
 * <h3>Boundary enforcement</h3>
 * When an entity would exceed a world boundary, its position is clamped to the surface and the
 * velocity component perpendicular to that face is zeroed, producing a "wall slide" effect.
 */
@Slf4j
public class OpenWorldEngine implements DynamicWorldEngine {

  @Override
  public void applyIntents(DynamicWorldState state, float deltaTime) {
    for (Map.Entry<String, Vector3> entry : state.moveIntents().entrySet()) {
      String name = entry.getKey();
      Vector3 intent = entry.getValue();

      OpenWorldEntity entity = state.entities().get(name);
      if (entity == null) {
        log.warn("[OpenWorldEngine] No entity for intent key '{}' — skipping.", name);
        continue;
      }

      // Normalise direction; skip if zero vector (client sent a no-op)
      Vector3 direction = intent.normalize();
      if (direction.equals(Vector3.ZERO)) {
        continue;
      }

      // v_new = v + direction * accel * dt, then clamp to maxSpeed
      Velocity3 newVelocity = entity.getVelocity()
          .add(direction.scale(entity.acceleration() * deltaTime))
          .clampSpeed(entity.maxSpeed());

      entity.setVelocity(newVelocity);
      entity.updatePosition(deltaTime);
      clampToBounds(entity, state.dimension());

      log.debug("[OpenWorldEngine] {} pos={} vel={}", name, entity.getPosition(),
          entity.getVelocity());
    }

    state.moveIntents().clear();
  }

  @Override
  public WorldResult storeMoveIntent(OpenMoveRequest req, DynamicWorldState state) {
    OpenWorldEntity entity = state.entities().get(req.playerId());
    if (entity == null) {
      String msg = String.format("[%s] storeMoveIntent: unknown entity '%s'.",
          state.worldName(), req.playerId());
      log.warn(msg);
      return failed(req.playerId(), state.worldName(), msg);
    }

    Vector3 moveIntent = Vector3.of(req.dx(), req.dy(), req.dz());
    state.moveIntents().put(req.playerId(), moveIntent);
    log.info("[OpenWorldEngine] Staged move intent for '{}': {}", req.playerId(), moveIntent);
    return succeeded(req.playerId(), state.worldName(), "intent staged");
  }

  @Override
  public WorldResult joinEntity(JoinRequest req, DynamicWorldState state) {
    if (state.entities().containsKey(req.playerName())) {
      return failed(req.playerName(), state.worldName(),
          String.format("Player '%s' is already in the world.", req.playerName()));
    }

    long id = IdSequenceGenerator.getInstance().nextPlayerEntityId();
    Vector3 spawn = randomSpawnPosition(state.dimension());
    OpenWorldPlayer player = new OpenWorldPlayer(req.playerName(), id, spawn);
    state.entities().put(player.getName(), player);

    String msg = String.format("Player '%s' joined '%s' at %s (id=%d).",
        player.getName(), state.worldName(), spawn, id);
    log.info("[OpenWorldEngine] {}", msg);
    return succeeded(player.getName(), state.worldName(), msg);
  }

  @Override
  public OpenWorldEntity removeEntity(String entityName, DynamicWorldState state) {
    OpenWorldEntity removed = state.entities().remove(entityName);
    if (removed != null) {
      state.moveIntents().remove(entityName);
      log.info("[OpenWorldEngine] Removed entity '{}' from '{}'.", entityName, state.worldName());
    } else {
      log.warn("[OpenWorldEngine] removeEntity: '{}' not found in '{}'.",
          entityName, state.worldName());
    }
    return removed;
  }

  @Override
  public String serialize(DynamicWorldState state) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");

    // bounds
    Dimension3D d = state.dimension();
    sb.append(String.format("\"bounds\": {\"width\": %d, \"height\": %d, \"depth\": %d},\n",
        d.getWidth(), d.getHeight(), d.getDepth()));

    // entities
    sb.append("\"entities\": {\n");
    int i = 0;
    int total = state.entities().size();
    for (OpenWorldEntity e : state.entities().values()) {
      Vector3 pos = e.getPosition();
      Velocity3 vel = e.getVelocity();
      sb.append(String.format(
          "\t\"%s\": {\n"
              + "\t\t\"x\": %.4f, \"y\": %.4f, \"z\": %.4f,\n"
              + "\t\t\"dx\": %.4f, \"dy\": %.4f, \"dz\": %.4f,\n"
              + "\t\t\"yaw\": %.4f, \"pitch\": %.4f\n"
              + "\t}",
          e.getName(),
          pos.getX(), pos.getY(), pos.getZ(),
          vel.getDx(), vel.getDy(), vel.getDz(),
          e.yaw(), e.pitch()));
      if (++i < total) {
        sb.append(",\n");
      }
    }
    sb.append("\n}\n}");
    return sb.toString();
  }

  @Override
  public String serializeOneLine(DynamicWorldState state) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");

    Dimension3D d = state.dimension();
    sb.append(String.format("\"bounds\":{\"width\":%d,\"height\":%d,\"depth\":%d},",
        d.getWidth(), d.getHeight(), d.getDepth()));

    sb.append("\"entities\":{");
    int i = 0;
    int total = state.entities().size();
    for (OpenWorldEntity e : state.entities().values()) {
      Vector3 pos = e.getPosition();
      Velocity3 vel = e.getVelocity();
      sb.append(String.format(
          "\"%s\":{\"x\":%.4f,\"y\":%.4f,\"z\":%.4f,"
              + "\"dx\":%.4f,\"dy\":%.4f,\"dz\":%.4f,"
              + "\"yaw\":%.4f,\"pitch\":%.4f}",
          e.getName(),
          pos.getX(), pos.getY(), pos.getZ(),
          vel.getDx(), vel.getDy(), vel.getDz(),
          e.yaw(), e.pitch()));
      if (++i < total) {
        sb.append(",");
      }
    }
    sb.append("}}");
    return sb.toString();
  }

  @Override
  public void reset(DynamicWorldState state) {
    state.entities().clear();
    state.moveIntents().clear();
    log.info("[OpenWorldEngine] World '{}' reset.", state.worldName());
  }

  /**
   * Picks a random spawn point on the "ground" plane (y = 0) within the XZ bounds of the world.
   */
  private Vector3 randomSpawnPosition(Dimension3D dim) {
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    float x = rng.nextFloat() * dim.getWidth();
    float z = rng.nextFloat() * dim.getDepth();
    return Vector3.of(x, 0f, z);
  }

  /**
   * Clamps the entity's position to the world bounding box and zeroes any velocity component
   * that would push it further out of bounds (wall-slide behaviour).
   */
  private void clampToBounds(OpenWorldEntity entity, Dimension3D dim) {
    Vector3 pos = entity.getPosition();
    Velocity3 vel = entity.getVelocity();

    float cx = clamp(pos.getX(), 0f, dim.getWidth());
    float cy = clamp(pos.getY(), 0f, dim.getHeight());
    float cz = clamp(pos.getZ(), 0f, dim.getDepth());

    // Zero out the velocity component for any clamped axis
    float vx = (cx != pos.getX()) ? 0f : vel.getDx();
    float vy = (cy != pos.getY()) ? 0f : vel.getDy();
    float vz = (cz != pos.getZ()) ? 0f : vel.getDz();

    if (cx != pos.getX() || cy != pos.getY() || cz != pos.getZ()) {
      entity.setPosition(Vector3.of(cx, cy, cz));
      entity.setVelocity(Velocity3.of(vx, vy, vz));
      log.debug("[OpenWorldEngine] Clamped '{}' to bounds: ({},{},{}).",
          entity.getName(), cx, cy, cz);
    }
  }

  private static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }
}
