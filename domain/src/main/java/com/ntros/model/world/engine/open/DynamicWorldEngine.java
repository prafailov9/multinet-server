package com.ntros.model.world.engine.open;

import com.ntros.model.world.state.open.DynamicWorldState;

public interface DynamicWorldEngine {

  void applyIntents(DynamicWorldState state, float deltaTime);

  void reset(DynamicWorldState state);

}
