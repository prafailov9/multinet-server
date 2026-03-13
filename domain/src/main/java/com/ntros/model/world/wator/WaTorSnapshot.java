package com.ntros.model.world.wator;

import java.util.List;

/**
 * Serialisable snapshot of the Wa-Tor world state broadcast to clients each tick window.
 *
 * <p>All entity coordinates are in world units (0–512). The client maps them to canvas
 * pixels using: {@code canvasPx = worldUnit × (canvasSize / worldSize)}.
 *
 * <p>The {@code "type": "wator"} field lets the client distinguish this frame from GoL
 * frames on the same WebSocket connection.
 */
public record WaTorSnapshot(
    List<AgentView>  predators,
    List<AgentView>  prey,
    List<PlantView>  plants,
    List<FoodView>   food
) {
  public String type() { return "wator"; }

  /**
   * A single creature's render data.
   *
   * @param id     entity ID (stable across frames for client-side interpolation)
   * @param x      world-unit X position
   * @param y      world-unit Y position
   * @param angle  heading in radians
   * @param hp     normalised HP (0–1)
   * @param energy normalised energy (0–1)
   */
  public record AgentView(int id, float x, float y, float angle, float hp, float energy) {}

  /**
   * A plant's render data.
   *
   * @param id   entity ID
   * @param x    world-unit X
   * @param y    world-unit Y
   * @param size growth stage (1–5); client scales the shape accordingly
   */
  public record PlantView(int id, float x, float y, float size) {}

  /**
   * A food entity's render data.
   *
   * @param id  entity ID
   * @param x   world-unit X
   * @param y   world-unit Y
   * @param ttl remaining TTL fraction (0–1); client fades the dot as it decays
   */
  public record FoodView(int id, float x, float y, float ttl) {}
}
