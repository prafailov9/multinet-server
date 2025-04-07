package com.ntros.model.entity;



public interface Entity {

    Position getPosition();
    void setPosition(Position position);
    String getName();
    MovementStrategy getMovementStrategy();

}
