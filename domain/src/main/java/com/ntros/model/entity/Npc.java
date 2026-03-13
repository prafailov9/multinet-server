package com.ntros.model.entity;

import com.ntros.model.entity.movement.grid.Position;
import com.ntros.model.entity.sequence.IdSequenceGenerator;

public abstract class Npc extends AbstractEntity {

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
