package com.ntros.model.world.connector.ops;

import com.ntros.model.world.protocol.request.OpenMoveRequest;

/**
 * World operation that carries a free-movement intent for an open 3D world.
 *
 * <p>Submitted by the session layer when a player sends an
 * {@link com.ntros.model.world.protocol.request.OpenMoveRequest}. The engine stores the
 * normalised direction as a thrust intent and integrates it into the entity's velocity on the
 * next tick.
 */
public record OpenMoveOp(OpenMoveRequest req) implements OpenWorldOp {

}
