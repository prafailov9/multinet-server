package com.ntros.model.entity;


import com.ntros.model.world.WorldContext;

public interface Entity {

    long getId();
    Position getPosition();
    void setPosition(Position position);
    String getName();
    MovementStrategy getMovementStrategy();
    void tick(WorldContext worldContext);

}
