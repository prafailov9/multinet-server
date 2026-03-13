package com.ntros.model.world.wator.component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fixed-topology neural-network genome for predator / prey agents.
 *
 * <h3>Network topology (fixed, never changes)</h3>
 * <pre>
 *   Inputs  (12): zone_type[0..4], zone_dist[0..4], own_energy, own_health
 *   Hidden  (16): tanh activations
 *   Outputs  (2): angular_velocity (tanh → ×MAX_ANGULAR_VEL),
 *                 linear_velocity  (sigmoid → ×MAX_SPEED)
 * </pre>
 * All weights are stored flat in a single {@code float[]} for cache-friendly forward-pass
 * access and simple mutation / crossover.
 *
 * <h3>Weight layout</h3>
 * <pre>
 *   [0   .. 191]  hidden layer weights  (INPUT_SIZE × HIDDEN_SIZE = 12×16)
 *   [192 .. 207]  hidden layer biases   (HIDDEN_SIZE = 16)
 *   [208 .. 239]  output layer weights  (HIDDEN_SIZE × OUTPUT_SIZE = 16×2)
 *   [240 .. 241]  output layer biases   (OUTPUT_SIZE = 2)
 * </pre>
 * Total: 242 floats per creature.
 */
public final class GenomeComponent {

  // ── Topology constants ────────────────────────────────────────────────────

  public static final int INPUT_SIZE  = 12;   // 5×(type+dist) + energy + health
  public static final int HIDDEN_SIZE = 16;
  public static final int OUTPUT_SIZE = 2;    // angular vel, linear vel

  // Weight-array index boundaries
  public static final int W1_START = 0;
  public static final int W1_END   = INPUT_SIZE * HIDDEN_SIZE;                 // 192
  public static final int B1_START = W1_END;
  public static final int B1_END   = B1_START + HIDDEN_SIZE;                   // 208
  public static final int W2_START = B1_END;
  public static final int W2_END   = W2_START + HIDDEN_SIZE * OUTPUT_SIZE;     // 240
  public static final int B2_START = W2_END;
  public static final int GENOME_SIZE = B2_START + OUTPUT_SIZE;                // 242

  // ── Mutation constants ────────────────────────────────────────────────────

  /** Fraction of weights that are perturbed during each mutation event. */
  public static final float MUTATION_RATE    = 0.15f;

  /** Standard deviation of Gaussian noise applied to each perturbed weight. */
  public static final float MUTATION_STRENGTH = 0.2f;

  // ── Data ──────────────────────────────────────────────────────────────────

  public final float[] weights;

  // ── Constructors ──────────────────────────────────────────────────────────

  /** Creates a genome with random weights in [-0.5, 0.5]. */
  public GenomeComponent() {
    weights = new float[GENOME_SIZE];
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    for (int i = 0; i < GENOME_SIZE; i++) {
      weights[i] = (float) (rng.nextDouble() - 0.5);
    }
  }

  /** Copy constructor (used for offspring before mutation). */
  public GenomeComponent(float[] weights) {
    this.weights = weights.clone();
  }

  // ── Evolution ─────────────────────────────────────────────────────────────

  /**
   * Returns a new genome that is a mutated copy of this one.
   * Each weight is independently perturbed with probability {@link #MUTATION_RATE}.
   */
  public GenomeComponent mutate() {
    float[] child = weights.clone();
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    for (int i = 0; i < GENOME_SIZE; i++) {
      if (rng.nextDouble() < MUTATION_RATE) {
        child[i] += (float) (rng.nextGaussian() * MUTATION_STRENGTH);
      }
    }
    return new GenomeComponent(child);
  }

  /**
   * Returns a new genome by combining this genome with {@code other} at a random split point.
   * The result is then mutated before being returned.
   */
  public GenomeComponent crossover(GenomeComponent other) {
    float[] child = new float[GENOME_SIZE];
    int split = ThreadLocalRandom.current().nextInt(GENOME_SIZE);
    System.arraycopy(this.weights,  0, child, 0,     split);
    System.arraycopy(other.weights, split, child, split, GENOME_SIZE - split);
    return new GenomeComponent(child).mutate();
  }
}
