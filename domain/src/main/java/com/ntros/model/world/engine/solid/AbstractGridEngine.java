package com.ntros.model.world.engine.solid;

import com.ntros.model.world.protocol.TileType;
import com.ntros.model.world.state.GridSnapshot;
import com.ntros.model.world.state.GridSnapshot.EntityView;
import com.ntros.model.world.state.GridState;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractGridEngine implements WorldEngine {

  @Override
  public Object snapshot(GridState state) {
    // tiles: "x,y" -> tile type name — EMPTY tiles are excluded.
    // The client treats absent keys as EMPTY, so omitting them keeps the
    // JSON payload small (critical for large GoL worlds that are mostly empty).
    Map<String, String> tiles = new LinkedHashMap<>();
    state.terrain().forEach((pos, tile) -> {
      if (tile != TileType.EMPTY) {
        tiles.put((int) pos.getX() + "," + (int) pos.getY(), tile.name());
      }
    });

    // entities: "entityName" -> {x,y}
    Map<String, EntityView> ents = new LinkedHashMap<>();
    state.entities().forEach((name, entity) -> {
      ents.put(name,
          new EntityView(entity.getPosition().getX(), entity.getPosition().getY()));
    });
    return new GridSnapshot(tiles, ents);
  }

}
