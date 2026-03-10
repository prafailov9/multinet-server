package com.ntros.model.entity.dynamic;

import com.ntros.model.entity.movement.Vector2D;
import com.ntros.model.entity.movement.Velocity2D;

public class DynamicPlayer extends AbstractDynamicEntity {

  private static final float ACCELERATION = 10f;
  private static final float MAX_SPEED = 5f;

  public DynamicPlayer(Vector2D position, Velocity2D velocity) {
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
  public Vector2D getPosition() {
    return position;
  }

  @Override
  public void setPosition(Vector2D position) {
    this.position = position;
  }
}