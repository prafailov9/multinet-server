package com.ntros.model.world.factory;

import com.ntros.model.world.engine.GridWorldEngine;
import com.ntros.model.world.engine.WorldEngine;
import com.ntros.model.world.state.GridWorldState;
import com.ntros.model.world.state.WorldState;

public class GridWorldFactory implements WorldFactory {
    private final String worldName;
    private final int width;
    private final int height;

    public GridWorldFactory(String worldName, int width, int height) {
        this.worldName = worldName;
        this.width = width;
        this.height = height;
    }

    @Override
    public WorldState createState() {
        return new GridWorldState(worldName, width, height);
    }

    @Override
    public WorldEngine createEngine() {
        return new GridWorldEngine(); // stateless, reusable
    }
}
