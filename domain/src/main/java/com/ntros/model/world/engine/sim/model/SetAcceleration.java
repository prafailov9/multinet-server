package com.ntros.model.world.engine.sim.model;

public record SetAcceleration(VehicleId id, double aMps2) implements TrafficIntent {}
