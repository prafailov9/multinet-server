package com.ntros.model.entity.dynamic;

import com.ntros.model.entity.movement.vectors.Vector;
import com.ntros.model.entity.movement.vectors.Vector2;
import com.ntros.model.entity.movement.velocity.Velocity;

public interface DynamicEntity {

  String getName();

  Vector getDynamicPosition();

  Velocity getVelocity();

  void setVelocity(Velocity velocity);

  void updatePosition(float deltaTime);

  Vector2 getPosition();

  void setPosition(Vector2 position);

  float rotation(); // angle in degrees

  float acceleration();

  float maxSpeed();

}
