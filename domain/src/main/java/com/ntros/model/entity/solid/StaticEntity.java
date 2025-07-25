package com.ntros.model.entity.solid;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.Position;

public interface StaticEntity extends Entity {

  void setPosition(Position position);

}
