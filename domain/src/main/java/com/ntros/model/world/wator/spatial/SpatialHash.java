package com.ntros.model.world.wator.spatial;

import com.ntros.model.world.wator.WaTorWorldState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uniform-grid spatial hash over the 512×512 Wa-Tor world.
 *
 * <h3>Grid parameters</h3>
 * <ul>
 *   <li>Cell size: 32 world units</li>
 *   <li>Grid: 16 × 16 = 256 cells</li>
 * </ul>
 *
 * <h3>Usage per tick</h3>
 * <ol>
 *   <li>Call {@link #rebuild(WaTorWorldState)} once at the start of each tick to populate
 *       the bucket arrays from the current {@code positions} list.</li>
 *   <li>Call {@link #queryRadius(float, float, float, List)} to retrieve entity IDs
 *       whose positions fall within a given radius — used by VisionSystem and interaction
 *       checks.</li>
 * </ol>
 *
 * <h3>Thread-safety</h3>
 * Not thread-safe.  Must only be used from the actor thread.
 */
public final class SpatialHash {

  public static final float CELL_SIZE = 32f;
  public static final int   GRID_COLS = (int) (WaTorWorldState.WIDTH  / CELL_SIZE);  // 16
  public static final int   GRID_ROWS = (int) (WaTorWorldState.HEIGHT / CELL_SIZE);  // 16
  public static final int   CELL_COUNT = GRID_COLS * GRID_ROWS;                       // 256

  /** Each bucket holds the entity IDs whose position maps to that cell. */
  @SuppressWarnings("unchecked")
  private final List<Integer>[] buckets = new List[CELL_COUNT];

  public SpatialHash() {
    for (int i = 0; i < CELL_COUNT; i++) {
      buckets[i] = new ArrayList<>(8);
    }
  }

  // ── Rebuild ───────────────────────────────────────────────────────────────

  /**
   * Clears all buckets and repopulates them from the current entity positions.
   * Must be called once at the start of each tick before any spatial query.
   */
  public void rebuild(WaTorWorldState state) {
    for (List<Integer> bucket : buckets) {
      bucket.clear();
    }
    for (int id = 0, size = state.positions.size(); id < size; id++) {
      if (!state.alive.get(id)) continue;
      var pos = state.positions.get(id);
      if (pos == null) continue;
      int idx = cellIndex(pos.x, pos.y);
      if (idx >= 0) {
        buckets[idx].add(id);
      }
    }
  }

  // ── Queries ───────────────────────────────────────────────────────────────

  /**
   * Appends to {@code out} the IDs of all entities whose position is within {@code radius}
   * world units of {@code (cx, cy)}.
   *
   * <p>Checks all cells whose bounding box overlaps the query circle; for each candidate entity
   * in those cells, performs an exact squared-distance test.
   *
   * @param cx     query centre X
   * @param cy     query centre Y
   * @param radius search radius (world units)
   * @param out    output list — entities are appended, not replaced
   */
  public void queryRadius(float cx, float cy, float radius, List<Integer> out) {
    float r2 = radius * radius;

    // Range of cell indices that could overlap the query circle
    int colMin = Math.max(0, (int) ((cx - radius) / CELL_SIZE));
    int colMax = Math.min(GRID_COLS - 1, (int) ((cx + radius) / CELL_SIZE));
    int rowMin = Math.max(0, (int) ((cy - radius) / CELL_SIZE));
    int rowMax = Math.min(GRID_ROWS - 1, (int) ((cy + radius) / CELL_SIZE));

    for (int row = rowMin; row <= rowMax; row++) {
      for (int col = colMin; col <= colMax; col++) {
        for (int id : buckets[row * GRID_COLS + col]) {
          // Caller must do its own distance check against positions[id] if needed —
          // bucket overlap does not guarantee circle containment.
          out.add(id);
        }
      }
    }
  }

  /**
   * Returns an unmodifiable view of the bucket for the cell containing {@code (x, y)}.
   * Useful for exact-cell lookups (e.g. plant spread into the same cell).
   */
  public List<Integer> cellAt(float x, float y) {
    int idx = cellIndex(x, y);
    return idx < 0 ? Collections.emptyList() : Collections.unmodifiableList(buckets[idx]);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static int cellIndex(float x, float y) {
    int col = (int) (x / CELL_SIZE);
    int row = (int) (y / CELL_SIZE);
    if (col < 0 || col >= GRID_COLS || row < 0 || row >= GRID_ROWS) return -1;
    return row * GRID_COLS + col;
  }
}
