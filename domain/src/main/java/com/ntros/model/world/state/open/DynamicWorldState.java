package com.ntros.model.world.state.open;

import com.ntros.model.entity.dynamic.DynamicEntity;
import com.ntros.model.entity.movement.Vector;
import com.ntros.model.world.state.dimension.Dimension;

import java.util.Map;

public interface DynamicWorldState {


    String worldName();
    Dimension dimension();
    Map<String, DynamicEntity> entities();
    Map<String, Vector> moveIntents();

}
