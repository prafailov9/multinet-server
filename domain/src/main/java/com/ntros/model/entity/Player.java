package com.ntros.model.entity;

import com.ntros.model.world.WorldContext;

public class Player extends AbstractMovableEntity {

    public Player(Position position) {
        super(position);
    }
    public Player(Position position, String name, long id, int hp) {
        super(position, name, id, hp);
    }

    @Override
    public void setPosition(Position position) {
        this.currentPosition = position;
    }

    @Override
    public void tick(WorldContext worldContext) {
        // no-op, movement is done by clients.
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", hp=" + hp +
                ", name='" + name + '\'' +
                ", currentPosition=" + currentPosition +
                ", movementIntent=" + movementIntent +
                ", movementStrategy=" + movementStrategy +
                '}';
    }
}
