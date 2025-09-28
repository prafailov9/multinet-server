package com.ntros.model.world.engine.sim.model;

public record SetDesiredSpeed(VehicleId id, double vMps)   implements TrafficIntent {}
