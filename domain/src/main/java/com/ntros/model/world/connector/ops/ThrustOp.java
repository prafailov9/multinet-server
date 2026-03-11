package com.ntros.model.world.connector.ops;

/**
 * @deprecated Replaced by {@link OpenMoveOp}, which carries a full 3D direction
 *             and wraps the typed {@link com.ntros.model.world.protocol.request.OpenMoveRequest}.
 *             Kept for reference; not part of the {@link OpenWorldOp} hierarchy.
 */
@Deprecated
public record ThrustOp(String entityId, float x, float y) {

}
