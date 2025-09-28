package com.ntros.model.world.engine.sim.model;

import java.util.Deque;

public class Vehicle {

  private final VehicleId id;
  private LaneId lane;            // current lane
  private double s;               // meters along lane
  private double v;               // m/s
  private final double length;    // meters
  private final Deque<LinkId> route; // remaining route (optional)

  public Vehicle(VehicleId id, LaneId lane, double s, double v, double length, Deque<LinkId> route) {
    this.id = id;
    this.lane = lane;
    this.s = s;
    this.v = v;
    this.length = length;
    this.route = route;
  }

  public VehicleId id() { return id; }
  public LaneId lane() { return lane; }
  public double s() { return s; }
  public double v() { return v; }
  public double length() { return length; }
  public Deque<LinkId> route() { return route; }

  // package-private setters so only the engine mutates
  void setLane(LaneId lane) { this.lane = lane; }
  void setS(double s) { this.s = s; }
  void setV(double v) { this.v = v; }

}
