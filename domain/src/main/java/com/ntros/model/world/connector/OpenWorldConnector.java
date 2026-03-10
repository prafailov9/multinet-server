package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.engine.open.OpenWorldEngine;
import com.ntros.model.world.protocol.ServerResult;
import com.ntros.model.world.state.open.OpenWorldState;
import java.util.List;

public class OpenWorldConnector implements WorldConnector {

  private final OpenWorldState state;
  private final OpenWorldEngine engine;

  public OpenWorldConnector(OpenWorldState state, OpenWorldEngine engine) {
    this.state = state;
    this.engine = engine;
  }

  @Override
  public ServerResult apply(WorldOp op) {
//    return switch (op) {
//      case JoinOp j -> engine.join(j.req(), state);
//      case ThrustOp t -> engine.storeIntent(t.req(), state);
//      case RemoveOp r -> {
//        engine.remove(r.removeRequest().entityId(), state);
//        yield CommandResult.succeeded(...);
//      }
//    };
    return null;
  }

  //public void update(DynamicWorldState state, float dt) {
  //  for (var e : state.entities().values()) {
  //      DynamicEntity entity = e;
  //      Vector intent = state.moveIntents().get(entity.getName());
  //      if (intent != null) {
  //          Vector2D dir = Vector2D.of(intent.getX(), intent.getY()).normalize();
  //          Velocity2D vel = (Velocity2D) entity.getVelocity();
  //          Velocity2D newVel = vel.add(dir.scale(entity.acceleration() * dt));
  //          entity.setVelocity(newVel);
  //      }
  //      entity.updatePosition(dt);
  //  }
  //  state.moveIntents().clear();
  //}

  @Override
  public void update() {
//    engine.update(state, 0.016f);
  }

  @Override
  public Object snapshot() {
    return null;
  }

  @Override
  public String snapshot(boolean oneLine) {
    return "";
  }

  @Override
  public String getWorldName() {
    return "";
  }

  @Override
  public String getWorldType() {
    return "";
  }

  @Override
  public List<Entity> getCurrentEntities() {
    return List.of();
  }

  @Override
  public WorldCapabilities getCapabilities() {
    return null;
  }

  @Override
  public void reset() {

  }

}