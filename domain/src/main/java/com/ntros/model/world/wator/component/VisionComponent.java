package com.ntros.model.world.wator.component;

/**
 * Per-creature sensor output produced by the {@code VisionSystem} each tick.
 *
 * <h3>Ray zone layout</h3>
 * Rays are cast into 5 angular zones relative to the creature's heading.
 * Each zone reports the closest intersecting entity (if any):
 * <pre>
 *   Zone 0 — far-left   : [−π,   −3π/5)
 *   Zone 1 — left       : [−3π/5, −π/5)
 *   Zone 2 — forward    : [−π/5,  +π/5)    ← widened for prey
 *   Zone 3 — right      : [+π/5, +3π/5)
 *   Zone 4 — far-right  : [+3π/5, +π]
 * </pre>
 *
 * <h3>Encoded values (per zone, 2 floats)</h3>
 * <ul>
 *   <li>{@code zoneType[i]}  — entity type encoded as float:
 *       0.0 = empty, 0.25 = food, 0.5 = plant, 0.75 = prey, 1.0 = predator</li>
 *   <li>{@code zoneDist[i]}  — normalized distance: 0.0 (right here) → 1.0 (at max range)</li>
 * </ul>
 *
 * <h3>NN inputs</h3>
 * The 10 zone values plus the creature's own {@code normalizedEnergy} and
 * {@code normalizedHealth} form the full 12-element input vector fed to the brain each tick.
 */
public final class VisionComponent {

  public static final int ZONE_COUNT = 5;

  /** Max ray length in world units. */
  public final float rayLength;

  /** Angle of the full field of view in radians (total arc covered by all 5 zones). */
  public final float fieldOfView;

  /** Per-zone closest entity type, float-encoded (0.0–1.0). */
  public final float[] zoneType = new float[ZONE_COUNT];

  /** Per-zone closest entity normalized distance (0.0–1.0). */
  public final float[] zoneDist = new float[ZONE_COUNT];

  /**
   * @param rayLength   max detection range in world units
   * @param fieldOfView total arc in radians (predators: π, prey: 4π/3 wider forward)
   */
  public VisionComponent(float rayLength, float fieldOfView) {
    this.rayLength   = rayLength;
    this.fieldOfView = fieldOfView;
  }

  /** Clears all zone readings to "empty" at the start of each VisionSystem pass. */
  public void reset() {
    for (int i = 0; i < ZONE_COUNT; i++) {
      zoneType[i] = 0f;
      zoneDist[i] = 1f;   // 1.0 = nothing detected (max distance)
    }
  }
}
