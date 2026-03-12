package com.ntros.model.entity;


import com.ntros.model.entity.movement.cell.Position;

public interface Entity {

  String getName();

  Position getPosition();

  void setPosition(Position position);


}
