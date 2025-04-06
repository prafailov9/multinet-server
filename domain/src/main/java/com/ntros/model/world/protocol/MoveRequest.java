package com.ntros.model.world.protocol;

import com.ntros.model.entity.Direction;

public record MoveRequest(String playerId, Direction direction) {


}
