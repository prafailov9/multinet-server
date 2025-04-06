package com.ntros.model.entity;

public abstract class AbstractMovementStrategy implements MovementStrategy {

    protected Direction intendedDirection;

    @Override
    public void setDirection(Direction direction) {
        intendedDirection = direction;
    }
}
