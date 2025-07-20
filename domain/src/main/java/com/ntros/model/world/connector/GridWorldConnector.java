package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.Result;
import com.ntros.model.world.state.solid.GridWorldState;

import java.util.List;

public class GridWorldConnector implements WorldConnector {

    private final GridWorldState gridWorldState;
    private final GridWorldEngine gridWorldEngine;

    public GridWorldConnector(GridWorldState gridWorldState, GridWorldEngine gridWorldEngine) {
        this.gridWorldState = gridWorldState;
        this.gridWorldEngine = gridWorldEngine;
    }


    @Override
    public void update() {
        gridWorldEngine.tick(gridWorldState);
    }

    @Override
    public Result storeMoveIntent(MoveRequest move) {
        return gridWorldEngine.storeMoveIntent(move, gridWorldState);
    }

    @Override
    public Result add(JoinRequest joinRequest) {
        return gridWorldEngine.add(joinRequest, gridWorldState);
    }

    @Override
    public void remove(String entityId) {
        gridWorldEngine.remove(entityId, gridWorldState);
    }

    @Override
    public String serialize() {
        return gridWorldEngine.serializeOneLine(gridWorldState);
    }

    @Override
    public String worldName() {
        return gridWorldState.worldName();
    }

    @Override
    public List<Entity> getCurrentEntities() {
        return gridWorldState.entities().values().stream().toList();
    }

    @Override
    public void reset() {
        gridWorldEngine.reset(gridWorldState);
    }

}
