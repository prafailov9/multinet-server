package com.ntros.model.world.connector.ops;

public sealed interface OpenWorldOp extends WorldOp permits JoinOp, ThrustOp, RemoveOp {

}
