package com.ntros.model.world.protocol.request;

import com.ntros.model.entity.Direction;
import com.ntros.model.entity.movement.MoveInput;

public record MoveRequest(String playerId, MoveInput moveInput) implements ClientRequest {


}
