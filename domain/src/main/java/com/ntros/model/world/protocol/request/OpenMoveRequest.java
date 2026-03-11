package com.ntros.model.world.protocol.request;

/**
 * A free-movement request for open 3D worlds.
 *
 * <p>The client sends a desired thrust direction expressed as a (dx, dy, dz) vector.
 * The engine normalises the direction and applies it against the entity's
 * {@code acceleration} and {@code maxSpeed} constraints each tick.
 *
 * <p>Unlike {@link MoveRequest}, which is discretised by {@link com.ntros.model.entity.Direction},
 * {@code OpenMoveRequest} carries a continuous 3D direction so the player can move freely in any
 * direction including vertically (e.g. jumping, flying).
 *
 * @param playerId unique name / identifier of the requesting player
 * @param dx       X-axis component of the desired movement direction (positive = east)
 * @param dy       Y-axis component of the desired movement direction (positive = up)
 * @param dz       Z-axis component of the desired movement direction (positive = south)
 */
public record OpenMoveRequest(
    String playerId,
    float  dx,
    float  dy,
    float  dz
) implements ClientRequest {

}
