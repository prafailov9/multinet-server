package com.ntros.model.world.state;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.protocol.TileType;
import java.util.Map;

@JsonSerialize(using = TerrainSerializer.class)
public record GridSnapshot(
    Map<Vector4, TileType> terrain,
    Map<String, EntityView> entities
) {

}