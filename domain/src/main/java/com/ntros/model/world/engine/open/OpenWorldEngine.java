package com.ntros.model.world.engine.open;

import com.ntros.model.entity.dynamic.DynamicEntity;
import com.ntros.model.entity.movement.Vector;
import com.ntros.model.entity.movement.Vector2D;
import com.ntros.model.entity.movement.Velocity2D;
import com.ntros.model.world.state.open.DynamicWorldState;

public class OpenWorldEngine implements DynamicWorldEngine {

  @Override
  public void applyIntents(DynamicWorldState state, float deltaTime) {

    for (var entry : state.moveIntents().entrySet()) {

      String id = entry.getKey();
      Vector intent = entry.getValue();

      DynamicEntity entity = state.entities().get(id);
      if (entity == null) {
        continue;
      }

      Vector2D direction = Vector2D.of(intent.getX(), intent.getY()).normalize();

      Velocity2D velocity = (Velocity2D) entity.getVelocity();

      float accel = entity.acceleration();

      Velocity2D newVelocity = velocity.add(
          direction.scale(accel * deltaTime)
      );

      // clamp speed
      float maxSpeed = entity.maxSpeed();

      Vector2D velVec = Vector2D.of(newVelocity.getDx(), newVelocity.getDy());

      if (velVec.magnitude() > maxSpeed) {
        velVec = velVec.normalize().scale(maxSpeed);
        newVelocity = Velocity2D.of(velVec.getX(), velVec.getY());
      }

      entity.setVelocity(newVelocity);

      entity.updatePosition(deltaTime);
    }

    state.moveIntents().clear();
  }

  @Override
  public void reset(DynamicWorldState state) {
    state.entities().clear();
    state.moveIntents().clear();
  }
}