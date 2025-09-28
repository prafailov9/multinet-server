package com.ntros.model.world.engine.sim.model;

import java.util.List;

public record SetRoute(VehicleId id, List<LinkId> route)   implements TrafficIntent {}
