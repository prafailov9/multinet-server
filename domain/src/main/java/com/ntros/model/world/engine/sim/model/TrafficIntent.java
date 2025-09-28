package com.ntros.model.world.engine.sim.model;


public sealed interface TrafficIntent permits
    SetAcceleration, SetDesiredSpeed, SetRoute, ChangeLane, SpawnVehicle, DespawnVehicle {

}

