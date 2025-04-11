package com.ntros.model.entity.dynamic;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.Vector;
import com.ntros.model.entity.movement.Velocity;

public interface DynamicEntity extends Entity {

    Vector getDynamicPosition();

    Velocity getVelocity();

    void setVelocity(Velocity velocity);

    void updatePosition(float deltaTime);

    float rotation(); // angle in degrees

    float acceleration();

    float maxSpeed();

}
