package com.ntros.model.world.factory;

import com.ntros.model.world.engine.WorldEngine;
import com.ntros.model.world.state.WorldState;

public interface WorldFactory {
    WorldState createState();

    WorldEngine createEngine();

}
