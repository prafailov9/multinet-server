package com.ntros.model.entity.dynamic;

import com.ntros.model.entity.movement.Vector;
import com.ntros.model.entity.movement.Vector2D;
import com.ntros.model.entity.movement.Velocity;
import com.ntros.model.entity.movement.Velocity2D;

public abstract class AbstractDynamicEntity implements DynamicEntity {


  protected Vector2D position;
  protected Velocity2D velocity;

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
    if (velocity instanceof Velocity2D v) {
      this.velocity = v;
    } else {
      throw new IllegalArgumentException("Velocity must be Velocity2D");
    }
  }

  @Override
  public void updatePosition(float deltaTime) {
    float dx = velocity.getDx() * deltaTime;
    float dy = velocity.getDy() * deltaTime;

    position = Vector2D.of(
        position.getX() + dx,
        position.getY() + dy
    );
  }

  @Override
  public abstract float rotation();

  @Override
  public abstract float acceleration();

  @Override
  public abstract float maxSpeed();
}
