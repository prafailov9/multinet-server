package com.ntros.model.world.engine.solid;

import com.ntros.model.world.engine.core.GridEngine;
import com.ntros.model.world.state.EntityView;
import com.ntros.model.world.state.d2.grid.GridSnapshot;
import com.ntros.model.world.state.core.GridState;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractGridEngine implements GridEngine {


  @Override
  public Object snapshot(GridState state) {

    return new GridSnapshot(state.terrain(), buildEntityView(state));
  }

  protected Map<String, EntityView> buildEntityView(GridState state) {
    Map<String, EntityView> ents = new LinkedHashMap<>();
    state.entities().forEach((name, entity) -> ents.put(name,
        new EntityView(entity.getPosition().getX(), entity.getPosition().getY())));
    return ents;
  }

}
