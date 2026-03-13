package com.ntros.model.world.state;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.protocol.CellType;
import java.util.Map;

@JsonSerialize(using = TerrainSerializer.class)
public record GridSnapshot(
    Map<Vector4, CellType> terrain,
    Map<String, EntityView> entities
) {

}