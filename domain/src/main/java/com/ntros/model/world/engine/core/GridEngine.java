package com.ntros.model.world.engine.core;

import com.ntros.model.world.state.core.GridState;

public interface GridEngine {


  void applyIntents(GridState state);

  String serialize(GridState state);

  Object snapshot(GridState state);

  String serializeOneLine(GridState state);

  void reset(GridState state);
}
