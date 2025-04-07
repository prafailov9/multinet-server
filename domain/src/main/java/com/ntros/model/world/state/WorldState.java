package com.ntros.model.world.state;

import com.ntros.model.entity.Direction;
import com.ntros.model.entity.Entity;
import com.ntros.model.entity.Position;
import com.ntros.model.world.TileType;
import com.ntros.model.world.state.dimension.Dimension;

import java.util.Map;

public interface WorldState {

    String name();
    Dimension dimension();
    Map<String, Entity> entities();
    Map<Position, String> takenPositions();
    Map<String, Direction> moveIntents();
    Map<Position, TileType> terrain();
    TileType getTileTypeAt(Position pos);
    boolean isLegalMove(Position position);
    boolean isWithinBounds(Position position);

}
