package com.ntros.model.world.context;

import com.ntros.model.world.engine.WorldEngine;
import com.ntros.model.world.state.WorldState;

public interface WorldContext {
    WorldState state();

    WorldEngine engine();

}
