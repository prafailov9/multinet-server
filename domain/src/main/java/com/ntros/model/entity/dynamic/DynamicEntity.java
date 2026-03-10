package com.ntros.model.entity.dynamic;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.Vector;
import com.ntros.model.entity.movement.Vector2D;
import com.ntros.model.entity.movement.Velocity;

public interface DynamicEntity {

  String getName();

  Vector getDynamicPosition();

  Velocity getVelocity();

  void setVelocity(Velocity velocity);

  void updatePosition(float deltaTime);

  Vector2D getPosition();

  void setPosition(Vector2D position);

  float rotation(); // angle in degrees

  float acceleration();

  float maxSpeed();

}
