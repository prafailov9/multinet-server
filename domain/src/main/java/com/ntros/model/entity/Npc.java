package com.ntros.model.entity;

import com.ntros.model.entity.sequence.IdSequenceGenerator;

public class Npc extends AbstractMovableEntity {

    public Npc(Position position) {
        super(position);
        id = IdSequenceGenerator.getInstance().getNextNpcId();
        name = "npc-" + id;
        movementStrategy = new RandomMovementStrategy();
    }


    @Override
    public void setPosition(Position position) {
        this.currentPosition = position;
    }

}
