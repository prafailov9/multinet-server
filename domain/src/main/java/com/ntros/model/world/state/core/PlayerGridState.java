package com.ntros.model.world.state.core;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.state.grid.CellType;
import java.util.Map;

public interface PlayerGridState extends Grid2DState {

  Map<String, Entity> entities();

  Map<Vector4, String> takenPositions();

  Map<String, Vector4> moveIntents();

  Map<Vector4, CellType> terrain();

  CellType getCellTypeAt(Vector4 pos);

  boolean isLegalMove(Vector4 position);

}
