package com.ntros.model.world.connector.ops;

import com.ntros.model.world.protocol.request.OrchestrateRequest;

/**
 * World operation emitted by an orchestrator client (e.g. Game of Life seed / toggle / clear).
 * Handled by world engines that support orchestration — others return a "not supported" result.
 */
public record OrchestrateOp(OrchestrateRequest req) implements GridWorldOp {

}
