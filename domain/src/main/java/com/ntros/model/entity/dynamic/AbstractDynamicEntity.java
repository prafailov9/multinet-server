package com.ntros.model.entity.dynamic;

import com.ntros.model.entity.movement.Vector;
import com.ntros.model.entity.movement.Vector2D;
import com.ntros.model.entity.movement.Velocity;
import com.ntros.model.entity.movement.Velocity2D;

public abstract class AbstractDynamicEntity implements DynamicEntity {


  protected final Vector2D position;
  protected final Velocity2D velocity;

  public AbstractDynamicEntity(Vector2D position, Velocity2D velocity) {
    this.position = position;
    this.velocity = velocity;
  }

  @Override
  public Vector getDynamicPosition() {
    return position;
  }

  @Override
  public Velocity getVelocity() {
    return velocity;
  }

  @Override
  public void setVelocity(Velocity velocity) {

  }

  @Override
  public void updatePosition(float deltaTime) {

  }

  @Override
  public float rotation() {
    return 0;
  }

  @Override
  public float acceleration() {
    return 0;
  }

  @Override
  public float maxSpeed() {
    return 0;
  }
}
