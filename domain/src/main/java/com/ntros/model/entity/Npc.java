package com.ntros.model.entity;

import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.entity.movement.Position;

public class Npc extends AbstractMovableEntity {

    public Npc(Position position) {
        super(position);
        id = IdSequenceGenerator.getInstance().getNextNpcId();
        name = "npc-" + id;
    }


    @Override
    public void setPosition(Position position) {
        this.currentPosition = position;
    }

}
