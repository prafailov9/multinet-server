package com.ntros.model.entity;

import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.WorldContext;

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

    @Override
    public void tick(WorldContext worldContext) {
        Position nextPosition = movementStrategy.decideNextPosition(currentPosition);
        if (worldContext.isLegalMove(nextPosition)) {
            movementStrategy.confirmChosenDirection();
        } else {
            movementStrategy.clearMovementIntent();
        }
    }

}
