package com.ntros.model.world.state;

import com.ntros.model.world.engine.sim.model.LaneId;
import com.ntros.model.world.engine.sim.model.LinkId;
import com.ntros.model.world.engine.sim.model.NodeId;
import com.ntros.model.world.engine.sim.model.RoadLink;
import com.ntros.model.world.engine.sim.model.RoadNode;
import com.ntros.model.world.engine.sim.model.Signal;
import com.ntros.model.world.engine.sim.model.Vehicle;
import com.ntros.model.world.engine.sim.model.VehicleId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only state for a traffic simulation. - No grid/tiles. Continuous lane-centric model. - All
 * mutating logic lives in the engine on the actor thread.
 */
public interface TrafficState extends CoreState {

  // --- Identity & meta ---
  @Override
  String worldName();

  @Override
  default String worldType() {
    return "TRAFFIC";
  }

  // --- Network snapshot (immutable views) ---
  Map<NodeId, RoadNode> nodes();                 // unmodifiable

  Map<LinkId, RoadLink> links();                 // unmodifiable

  Map<NodeId, Signal> signals();               // unmodifiable (Signal itself should expose read-only getters to callers)

  // --- Dynamic actors ---
  Map<VehicleId, Vehicle> vehicles();            // unmodifiable view

  // Optional index for quick lookups (engine populates/maintains)
  Map<LaneId, List<VehicleId>> laneOccupancy();  // vehicles ordered by s (front to back)

  // --- Timekeeping ---
  long tickNumber();          // how many steps have been applied

  double dtSeconds();         // step size used by engine for last/next update

  // --- Read helpers ---
  default Optional<Vehicle> findVehicle(VehicleId id) {
    return Optional.ofNullable(vehicles().get(id));
  }
}