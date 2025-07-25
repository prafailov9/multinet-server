package com.ntros.model.entity;

import com.ntros.model.entity.movement.Position;
import com.ntros.model.entity.solid.StaticEntity;

public abstract class AbstractMovableEntity implements StaticEntity {

  protected long id;
  protected int hp;
  protected String name;
  protected Position currentPosition;
  protected Direction movementIntent;

  public AbstractMovableEntity(Position position) {
    this.currentPosition = position;
  }

  public AbstractMovableEntity(Position position, String name, long id, int hp) {
    this.currentPosition = position;
    this.name = name;
    this.id = id;
    this.hp = hp;
  }

  @Override
  public Position getPosition() {
    return currentPosition;
  }

  @Override
  public String getName() {
    return name;
  }

}
