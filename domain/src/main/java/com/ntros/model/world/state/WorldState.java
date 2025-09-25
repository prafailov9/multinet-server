package com.ntros.model.world.state;

import com.ntros.model.entity.Direction;
import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.Position;
import com.ntros.model.world.protocol.TileType;
import com.ntros.model.world.state.dimension.Dimension;
import java.util.Map;

/**
 * Read-only world state abstraction.
 */
public interface WorldState {

  String worldName();
  String worldType();

  Dimension dimension();

  Map<String, Entity> entities();

  Map<Position, String> takenPositions();

  Map<String, Direction> moveIntents();

  Map<Position, TileType> terrain();

  TileType getTileTypeAt(Position pos);

  boolean isLegalMove(Position position);

  boolean isWithinBounds(Position position);

}
