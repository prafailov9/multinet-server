package com.ntros.model.entity.solid;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.Position;

public interface StaticEntity extends Entity {

    Position getPosition();
    void setPosition(Position position);

}
