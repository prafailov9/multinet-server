package com.ntros.model.world.protocol.request;

import com.ntros.model.entity.Direction;

public record MoveRequest(String playerId, Direction direction) implements ClientRequest {


}
