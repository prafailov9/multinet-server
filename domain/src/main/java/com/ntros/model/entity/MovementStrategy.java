package com.ntros.model.entity;

public interface MovementStrategy {

    boolean hasMovementIntent();
    Direction getDirection();
    Position getPosition();
    Position decideNextPosition(Position currentPosition);
    void setDirection(Direction direction);
    void confirmChosenDirection();
    void clearMovementIntent();

}
