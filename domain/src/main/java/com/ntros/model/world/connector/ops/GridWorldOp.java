package com.ntros.model.world.connector.ops;

public sealed interface GridWorldOp extends WorldOp permits JoinOp, MoveOp, RemoveOp, OrchestrateOp {

}
