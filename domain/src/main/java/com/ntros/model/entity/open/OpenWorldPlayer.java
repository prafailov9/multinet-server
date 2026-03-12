package com.ntros.model.entity.open;

import com.ntros.model.entity.movement.vectors.Vector3;
import com.ntros.model.entity.movement.velocity.Velocity3;

/**
 * A human-controlled entity in the open 3D world.
 *
 * <p>Movement characteristics are tuned for a player avatar:
 * <ul>
 *   <li><b>acceleration</b> 20 units/s² — snappy enough to feel responsive</li>
 *   <li><b>maxSpeed</b> 10 units/s — prevents runaway velocity build-up</li>
 * </ul>
 */
public class OpenWorldPlayer extends AbstractOpenWorldEntity {

  private static final float DEFAULT_ACCELERATION = 20f;
  private static final float DEFAULT_MAX_SPEED = 10f;

  private final String name;
  private final long id;

  public OpenWorldPlayer(String name, long id, Vector3 spawnPosition) {
    super(spawnPosition, Velocity3.ZERO);
    this.name = name;
    this.id = id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public float acceleration() {
    return DEFAULT_ACCELERATION;
  }

  @Override
  public float maxSpeed() {
    return DEFAULT_MAX_SPEED;
  }

  @Override
  public String toString() {
    return "OpenWorldPlayer{name='" + name + "', id=" + id
        + ", pos=" + position + ", vel=" + velocity + '}';
  }
}
