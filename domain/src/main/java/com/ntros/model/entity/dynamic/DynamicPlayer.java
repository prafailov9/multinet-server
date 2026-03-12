package com.ntros.model.entity.dynamic;

import com.ntros.model.entity.movement.vectors.Vector2;
import com.ntros.model.entity.movement.velocity.Velocity2;

public class DynamicPlayer extends AbstractDynamicEntity {

  private static final float ACCELERATION = 10f;
  private static final float MAX_SPEED = 5f;

  public DynamicPlayer(Vector2 position, Velocity2 velocity) {
    super(position, velocity);
  }

  @Override
  public float rotation() {
    return (float) Math.atan2(velocity.getDy(), velocity.getDx());
  }

  @Override
  public float acceleration() {
    return ACCELERATION;
  }

  @Override
  public float maxSpeed() {
    return MAX_SPEED;
  }

  @Override
  public String getName() {
    return "Player";
  }

  @Override
  public Vector2 getPosition() {
    return position;
  }

  @Override
  public void setPosition(Vector2 position) {
    this.position = position;
  }
}