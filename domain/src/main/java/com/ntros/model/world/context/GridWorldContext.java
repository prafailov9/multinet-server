package com.ntros.model.world.context;

import com.ntros.model.world.engine.GridWorldEngine;
import com.ntros.model.world.engine.WorldEngine;
import com.ntros.model.world.state.GridWorldState;
import com.ntros.model.world.state.WorldState;

public class GridWorldContext implements WorldContext {

    private final GridWorldState gridWorldState;
    private final GridWorldEngine gridWorldEngine;

    public GridWorldContext(GridWorldState gridWorldState, GridWorldEngine gridWorldEngine) {
        this.gridWorldState = gridWorldState;
        this.gridWorldEngine = gridWorldEngine;
    }


    @Override
    public WorldState state() {
        return gridWorldState;
    }

    @Override
    public WorldEngine engine() {
        return gridWorldEngine;
    }

}
