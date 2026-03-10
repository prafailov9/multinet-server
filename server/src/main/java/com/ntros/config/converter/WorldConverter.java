package com.ntros.config.converter;

import com.ntros.config.data.CfgCapabilities;
import com.ntros.config.data.CfgState;
import com.ntros.config.data.CfgWorld;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.state.solid.GridWorldState;

public class WorldConverter implements Converter<CfgWorld, WorldConnector> {

  @Override
  public CfgWorld toFileObject(WorldConnector modelObject) {
    return null;
  }

  @Override
  public WorldConnector toModelObject(CfgWorld cfgWorld) {
    if (cfgWorld.cfgState().dimensions().size() != 2) {
      throw new RuntimeException("3D and 4d worlds not defined yet.");
    }
    CfgState cfgState = cfgWorld.cfgState();
    CfgCapabilities cfgCaps = cfgWorld.cfgCapabilities();

    GridWorldState state = new GridWorldState(cfgState.name(), cfgState.dimensions().get(0),
        cfgState.dimensions().get(1));
    WorldCapabilities capabilities = new WorldCapabilities(cfgCaps.multiplayer(),
        cfgCaps.orchestrated(),
        cfgCaps.hasAi(), cfgCaps.deterministic());
    return new GridWorldConnector(state, new GridWorldEngine(), capabilities);
  }


}
