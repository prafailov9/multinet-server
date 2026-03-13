package com.ntros.model.world.wator.neural;

import com.ntros.model.world.wator.component.GenomeComponent;
import com.ntros.model.world.wator.component.VelocityComponent;
import com.ntros.model.world.wator.component.VisionComponent;

/**
 * Evaluates a {@link GenomeComponent} forward pass to produce motor commands.
 *
 * <h3>Input vector (12 elements)</h3>
 * <pre>
 *   [0..4]  zone_type[0..4]  — entity type float-encoded (0=empty, 0.25=food,
 *                               0.5=plant, 0.75=prey, 1.0=predator)
 *   [5..9]  zone_dist[0..4]  — normalized distance per zone (0=adjacent, 1=max range)
 *   [10]    own_energy        — current / max energy (0–1)
 *   [11]    own_hp            — current / max hp     (0–1)
 * </pre>
 *
 * <h3>Output vector (2 elements after activation)</h3>
 * <pre>
 *   [0]  angular_velocity  — tanh(raw) × MAX_ANGULAR_VEL  (rad/s, ±2π)
 *   [1]  linear_velocity   — sigmoid(raw) × MAX_SPEED     (world-units/s, 0–60)
 * </pre>
 */
public final class Brain {

  private Brain() {}

  /**
   * Runs one forward pass of the fixed-topology network encoded in {@code genome}
   * and writes the resulting motor commands into {@code vel}.
   *
   * @param genome  the weight array to evaluate
   * @param vision  sensor readings for the current tick
   * @param energy  normalised energy (0–1)
   * @param health  normalised health (0–1)
   * @param vel     velocity component to update in-place (no allocation)
   */
  public static void evaluate(GenomeComponent genome, VisionComponent vision,
      float energy, float health, VelocityComponent vel) {

    float[] w = genome.weights;

    // ── Build input vector ────────────────────────────────────────────────
    // Inline into the hidden-layer dot product to avoid allocating a float[12].
    final int N = GenomeComponent.INPUT_SIZE;   // 12
    final int H = GenomeComponent.HIDDEN_SIZE;  // 16
    final int O = GenomeComponent.OUTPUT_SIZE;  // 2

    // ── Hidden layer: h[j] = tanh( sum_i(input[i] * w1[i*H+j]) + b1[j] ) ─
    float[] hidden = new float[H];
    for (int j = 0; j < H; j++) {
      float sum = w[GenomeComponent.B1_START + j]; // bias
      // zone_type inputs (0..4)
      for (int z = 0; z < VisionComponent.ZONE_COUNT; z++) {
        sum += vision.zoneType[z] * w[GenomeComponent.W1_START + z * H + j];
      }
      // zone_dist inputs (5..9)
      for (int z = 0; z < VisionComponent.ZONE_COUNT; z++) {
        sum += vision.zoneDist[z] * w[GenomeComponent.W1_START + (VisionComponent.ZONE_COUNT + z) * H + j];
      }
      // own_energy (10)
      sum += energy * w[GenomeComponent.W1_START + 10 * H + j];
      // own_health (11)
      sum += health * w[GenomeComponent.W1_START + 11 * H + j];

      hidden[j] = tanh(sum);
    }

    // ── Output layer: out[k] = sum_j(hidden[j] * w2[j*O+k]) + b2[k] ──────
    float rawAngular = w[GenomeComponent.B2_START];      // bias for output 0
    float rawSpeed   = w[GenomeComponent.B2_START + 1];  // bias for output 1
    for (int j = 0; j < H; j++) {
      rawAngular += hidden[j] * w[GenomeComponent.W2_START + j * O];
      rawSpeed   += hidden[j] * w[GenomeComponent.W2_START + j * O + 1];
    }

    // ── Apply activations and write motor commands ────────────────────────
    // Angular velocity: tanh maps raw to (−1, 1), scale to ±MAX_ANGULAR_VEL
    vel.angularVel = tanh(rawAngular) * VelocityComponent.MAX_ANGULAR_VEL;

    // Linear speed: sigmoid maps raw to (0, 1), scale to [0, MAX_SPEED]
    vel.speed = sigmoid(rawSpeed) * VelocityComponent.MAX_SPEED;
  }

  // ── Fast scalar activations ───────────────────────────────────────────────

  private static float tanh(float x) {
    // Clamp to avoid overflow in Math.exp for very large inputs
    if (x >  8f) return  1f;
    if (x < -8f) return -1f;
    float e2x = (float) Math.exp(2.0 * x);
    return (e2x - 1f) / (e2x + 1f);
  }

  private static float sigmoid(float x) {
    if (x >  8f) return 1f;
    if (x < -8f) return 0f;
    return 1f / (1f + (float) Math.exp(-x));
  }
}
