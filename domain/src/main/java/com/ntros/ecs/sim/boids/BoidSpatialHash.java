package com.ntros.ecs.sim.boids;

import com.ntros.ecs.core.EntityId;
import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-size bucket grid for efficient O(k) neighbourhood queries in the boids simulation.
 *
 * <h3>Layout</h3>
 * The world is divided into square cells of size {@code cellSize}.
 * {@code bucket(bx, by) = by * cols + bx} — a flat 1-D array of {@link List}&lt;{@link EntityId}&gt;.
 *
 * <h3>Usage per tick</h3>
 * <ol>
 *   <li>Call {@link #clear()} at the start of each simulation step.</li>
 *   <li>Call {@link #insert} for every boid's current position.</li>
 *   <li>Call {@link #nearby} to gather candidates within a radius; the caller filters by exact
 *       distance.</li>
 * </ol>
 *
 * <h3>Complexity</h3>
 * Insert: O(1). Query: O(cells-in-radius-square), typically O(1) for small radii / large cells.
 */
public class BoidSpatialHash {

  private final float cellSize;
  private final int cols;
  private final int rows;

  @SuppressWarnings("unchecked")
  private final List<EntityId>[] buckets;

  /**
   * @param worldW   total world width (exclusive upper bound for x)
   * @param worldH   total world height (exclusive upper bound for y)
   * @param cellSize side length of each bucket cell
   */
  @SuppressWarnings("unchecked")
  public BoidSpatialHash(float worldW, float worldH, float cellSize) {
    this.cellSize = cellSize;
    this.cols = Math.max(1, (int) Math.ceil(worldW / cellSize));
    this.rows = Math.max(1, (int) Math.ceil(worldH / cellSize));
    buckets = new List[cols * rows];
    for (int i = 0; i < buckets.length; i++) {
      buckets[i] = new ArrayList<>(8);
    }
  }

  /** Removes all entries from every bucket. O(buckets). */
  public void clear() {
    for (List<EntityId> b : buckets) {
      b.clear();
    }
  }

  /**
   * Inserts {@code id} into the bucket covering world position {@code (x, y)}.
   *
   * <p>Coordinates are clamped to the valid bucket range so out-of-bounds entities are placed
   * at the nearest edge bucket rather than causing an array access exception.
   */
  public void insert(EntityId id, float x, float y) {
    int bx = Math.min((int) (x / cellSize), cols - 1);
    int by = Math.min((int) (y / cellSize), rows - 1);
    bx = Math.max(0, bx);
    by = Math.max(0, by);
    buckets[by * cols + bx].add(id);
  }

  /**
   * Returns all entity IDs whose bucket overlaps the square
   * {@code [x-radius, x+radius] × [y-radius, y+radius]}.
   *
   * <p>The list may include entities farther than {@code radius} — callers must check
   * exact Euclidean distance when needed.
   *
   * @return a newly allocated list containing all candidates
   */
  public List<EntityId> nearby(float x, float y, float radius) {
    int x0 = Math.max(0, (int) ((x - radius) / cellSize));
    int x1 = Math.min(cols - 1, (int) ((x + radius) / cellSize));
    int y0 = Math.max(0, (int) ((y - radius) / cellSize));
    int y1 = Math.min(rows - 1, (int) ((y + radius) / cellSize));

    List<EntityId> result = new ArrayList<>();
    for (int by = y0; by <= y1; by++) {
      for (int bx = x0; bx <= x1; bx++) {
        result.addAll(buckets[by * cols + bx]);
      }
    }
    return result;
  }
}
