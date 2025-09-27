package com.ntros.model.world.connector.ops;

import com.ntros.model.world.protocol.request.MoveRequest;

public record MoveOp(MoveRequest req) implements WorldOp {

}
