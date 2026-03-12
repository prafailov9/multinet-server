package com.ntros.model.entity.dynamic;

import com.ntros.model.entity.movement.vectors.Vector;
import com.ntros.model.entity.movement.vectors.Vector2;
import com.ntros.model.entity.movement.velocity.Velocity;
import com.ntros.model.entity.movement.velocity.Velocity2;

public abstract class AbstractDynamicEntity implements DynamicEntity {


  protected Vector2 position;
  protected Velocity2 velocity;

  public AbstractDynamicEntity(Vector2 position, Velocity2 velocity) {
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
    if (velocity instanceof Velocity2 v) {
      this.velocity = v;
    } else {
      throw new IllegalArgumentException("Velocity must be Velocity2D");
    }
  }

  @Override
  public void updatePosition(float deltaTime) {
    float dx = velocity.getDx() * deltaTime;
    float dy = velocity.getDy() * deltaTime;

    position = Vector2.of(
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
