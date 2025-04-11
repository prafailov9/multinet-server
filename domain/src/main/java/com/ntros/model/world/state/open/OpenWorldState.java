package com.ntros.model.world.state.open;

import com.ntros.model.entity.dynamic.DynamicEntity;
import com.ntros.model.entity.movement.Vector;
import com.ntros.model.world.state.dimension.Dimension;
import com.ntros.model.world.state.dimension.Dimension2D;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenWorldState implements DynamicWorldState {

    private final String worldName;
    private final Dimension dimension;
    private final Map<String, DynamicEntity> entities;
    private final Map<String, Vector> movementIntents;

    public OpenWorldState(String worldName, int width, int height) {
        this.worldName = worldName;
        this.dimension = new Dimension2D(width, height);

        entities = new LinkedHashMap<>(); // preserve insertion order
        movementIntents = new HashMap<>();


    }

    @Override
    public String worldName() {
        return worldName;
    }

    @Override
    public Dimension dimension() {
        return dimension;
    }

    @Override
    public Map<String, DynamicEntity> entities() {
        return entities;
    }


    @Override
    public Map<String, Vector> moveIntents() {
        return movementIntents;
    }

}
