package com.ntros.model.world.state;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.cell.Position;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.protocol.TileType;
import com.ntros.model.world.state.dimension.Dimension;
import java.util.Map;

public interface GridState extends CoreState {

  Dimension dimension();

  Map<String, Entity> entities();

  Map<Vector4, String> takenPositions();

  Map<String, Vector4> moveIntents();

  Map<Vector4, TileType> terrain();

  TileType getTileTypeAt(Vector4 pos);

  boolean isLegalMove(Vector4 position);

  boolean isWithinBounds(Vector4 position);
}