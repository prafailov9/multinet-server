package com.ntros.model.world.state;

import com.ntros.model.entity.Direction;
import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.Position;
import com.ntros.model.entity.solid.StaticEntity;
import com.ntros.model.world.TileType;
import com.ntros.model.world.state.dimension.Dimension;

import java.util.Map;

public interface WorldState {

    String worldName();
    Dimension dimension();
    Map<String, StaticEntity> entities();
    Map<Position, String> takenPositions();
    Map<String, Direction> moveIntents();
    Map<Position, TileType> terrain();
    TileType getTileTypeAt(Position pos);
    boolean isLegalMove(Position position);
    boolean isWithinBounds(Position position);

}
