package com.ntros.model.world.engine.sim.model;

public record RoadLink(
    LinkId id,
    NodeId from,
    NodeId to,
    double lengthMeters,
    int laneCount,
    double speedLimitMps
) {

}