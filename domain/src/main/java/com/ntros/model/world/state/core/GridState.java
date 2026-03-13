package com.ntros.model.world.state.core;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.state.d2.grid.CellType;
import com.ntros.model.world.state.dimension.Dimension;
import java.util.Map;

public interface GridState extends WorldState {

  Dimension dimension();

  Map<String, Entity> entities();

  Map<Vector4, String> takenPositions();

  Map<String, Vector4> moveIntents();

  Map<Vector4, CellType> terrain();

  CellType getTileTypeAt(Vector4 pos);

  boolean isLegalMove(Vector4 position);

  boolean isWithinBounds(Vector4 position);
}