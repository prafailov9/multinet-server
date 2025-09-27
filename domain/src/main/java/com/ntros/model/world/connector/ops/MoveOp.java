package com.ntros.model.world.connector.ops;

import com.ntros.model.world.protocol.MoveRequest;

public record MoveOp(MoveRequest req) implements WorldOp {

}
