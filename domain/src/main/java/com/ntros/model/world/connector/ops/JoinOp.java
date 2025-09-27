package com.ntros.model.world.connector.ops;

import com.ntros.model.world.protocol.request.JoinRequest;

public record JoinOp(JoinRequest req) implements WorldOp {

}

