package com.ntros.model.entity;

public abstract class AbstractMovableEntity implements Entity {

    protected long id;
    protected int hp;
    protected String name;
    protected Position currentPosition;
    protected Direction movementIntent;
    protected MovementStrategy movementStrategy;

    public AbstractMovableEntity(Position position) {
        this.currentPosition = position;
    }

    public AbstractMovableEntity(Position position, String name, long id, int hp) {
        this.currentPosition = position;
        this.name = name;
        this.id = id;
        this.hp = hp;
    }

    public AbstractMovableEntity(Position position, MovementStrategy movementStrategy) {
        this(position);
        this.movementStrategy = movementStrategy;
    }

    @Override
    public MovementStrategy getMovementStrategy() {
        return movementStrategy;
    }

    @Override
    public Position getPosition() {
        return currentPosition;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

}
