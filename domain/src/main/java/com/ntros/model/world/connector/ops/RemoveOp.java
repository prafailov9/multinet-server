package com.ntros.model.world.connector.ops;

import com.ntros.model.world.protocol.request.RemoveRequest;

public record RemoveOp(RemoveRequest removeRequest) implements WorldOp {

}

