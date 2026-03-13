package com.ntros.model.entity;

import com.ntros.model.entity.movement.grid.Position;

public abstract class AbstractEntity implements Entity {

  protected long id;
  protected int hp;
  protected String name;
  protected Position currentPosition;
  protected Position movementIntent;

  public AbstractEntity(Position position) {
    this.currentPosition = position;
  }

  public AbstractEntity(Position position, String name, long id, int hp) {
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
