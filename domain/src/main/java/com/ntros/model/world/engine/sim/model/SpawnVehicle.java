package com.ntros.model.world.engine.sim.model;

import java.util.List;

public record SpawnVehicle(
    VehicleId id,
    LaneId lane,
    double initialS,
    double initialV,
    double lengthMeters,
    List<LinkId> route
) implements TrafficIntent {}
