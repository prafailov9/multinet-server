package com.ntros.model.world.state.core;

import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.state.dimension.Dimension;

public interface GridState extends WorldState {

  Dimension dimension();

  boolean isWithinBounds(Vector4 position);
}