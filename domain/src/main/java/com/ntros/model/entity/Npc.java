package com.ntros.model.entity;

import com.ntros.model.entity.movement.Position;
import com.ntros.model.entity.sequence.IdSequenceGenerator;

public class Npc extends AbstractMovableEntity {

  public Npc(Position position) {
    super(position);
    id = IdSequenceGenerator.getInstance().nextNpcEntityId();
    name = "npc-" + id;
  }


  @Override
  public void setPosition(Position position) {
    this.currentPosition = position;
  }

}
