package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.engine.sim.TrafficEngine;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.model.world.state.TrafficState;
import java.util.List;

public final class TrafficConnector implements WorldConnector {

  private final TrafficState state;
  private final TrafficEngine engine;
  private final WorldCapabilities caps;

  public TrafficConnector(TrafficState state, TrafficEngine engine, WorldCapabilities caps) {
    this.state = state;
    this.engine = engine;
    this.caps = caps;
  }

  @Override
  public CommandResult apply(WorldOp op) {
//    return switch (op) {
//      case JoinOp j    -> {
//        // interpret JOIN as SpawnVehicle (choose lane/spawn point via policy)
//        var spawn = /* map JoinRequest → SpawnVehicle */;
//        yield engine.enqueueIntent(spawn, state);
//      }
//      case MoveOp m    -> {
//        // interpret MOVE (from your protocol) as e.g., ChangeLane or SetDesiredSpeed
//        // or keep MOVE for grid-only and add new protocol commands for traffic.
//        yield engine.enqueueIntent(/* map MoveRequest → TrafficIntent */, state);
//      }
//      case RemoveOp r  -> engine.enqueueIntent(new DespawnVehicle(new VehicleId(r.removeRequest().entityId())), state);
//    };
    return CommandResult.succeeded("", "", "");
  }

  @Override
  public void update() {
    engine.step(state, state.dtSeconds());
  }

  @Override
  public Object snapshot() {
    return null;
  }

  @Override
  public String snapshot(boolean oneLine) {
    return oneLine ? engine.serializeOneLine(state) : engine.serialize(state);
  }

  @Override
  public String getWorldName() {
    return state.worldName();
  }

  @Override
  public String getWorldType() {
    return state.worldType();
  }

  @Override
  public List<Entity> getCurrentEntities() { /* optional adapter if you expose vehicles as entities */
    return List.of();
  }

  @Override
  public WorldCapabilities getCapabilities() {
    return caps;
  }

  @Override
  public void reset() {
    engine.reset(state);
  }
}
