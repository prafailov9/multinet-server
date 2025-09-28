package com.ntros.model.world.engine.sim.model;

public record ChangeLane(VehicleId id, int delta)          implements TrafficIntent {} // -1 left, +1 right
