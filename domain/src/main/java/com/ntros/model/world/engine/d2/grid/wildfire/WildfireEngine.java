package com.ntros.model.world.engine.d2.grid.wildfire;

import static com.ntros.model.world.engine.d2.grid.wildfire.WildfireCell.ASH;
import static com.ntros.model.world.engine.d2.grid.wildfire.WildfireCell.BURNING;
import static com.ntros.model.world.engine.d2.grid.wildfire.WildfireCell.EMPTY;
import static com.ntros.model.world.engine.d2.grid.wildfire.WildfireCell.TREE;

import com.ntros.model.entity.movement.grid.Position;
import com.ntros.model.world.engine.core.SimulationGridEngine;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.core.SimulationGridState;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WildfireEngine implements SimulationGridEngine {

  private final static int BURN_DURATION = 5;
  private final static float BASE_SPREAD_PROBABILITY = 0.3f;
  private final static float INV_SQRT2 = 0.70710678f;

  private final Random rng = new Random();
  private WildfireCell[] current; // flat array, row-major: y*W + x
  private WildfireCell[] next;    // scratch buffer, swapped each tick
  private int[] burnTimer;        // per-cell: ticks remaining while BURNING
  private int[] nextBurnTimer;
  private float windDx, windDy;   // unit vector, wind blows this direction
  private float windSpeed;        // 0.0 = calm, 1.0 = gale
  private int W, H;
  private int size;
  private boolean needsFullSnapshot;

  // helpers index arrays
  // all 8 neighbors
  private final int[] neighX = {-1, 0, 1, -1, 1, -1, 0, 1};
  private final int[] neighY = {-1, -1, -1, 0, 0, 1, 1, 1};

  @Override
  public WorldResult orchestrate(OrchestrateRequest req, SimulationGridState state) {
    ensureBuffers(state);
    return switch (req.action()) {
      case RANDOM_SEED -> applyRandomSeed(req, state);
      case SEED -> applySeed(req, state);
      case PLACE -> applyPlace(req, state);
      case CLEAR -> applyClear(state);
      case SET_WIND -> applySetWind(req, state);
      default -> WorldResult.failed("orchestrator", state.worldName(),
          "Wildfire supports RANDOM_SEED, SEED, PLACE, CLEAR, SET_WIND.");
    };
  }

  // ── Orchestrate handlers ─────────────────────────────────────────────────────

  private WorldResult applyRandomSeed(OrchestrateRequest req, SimulationGridState state) {
    float density = Math.max(0f, Math.min(1f, req.density()));
    for (int i = 0; i < size; i++) {
      current[i] = rng.nextFloat() < density ? TREE : EMPTY;
      burnTimer[i] = 0;
    }
    needsFullSnapshot = true;
    log.info("[Wildfire] RANDOM_SEED '{}' ({}×{}, density={}).", state.worldName(), W, H, density);
    return WorldResult.succeeded("orchestrator", state.worldName(),
        String.format("Forest seeded at %.0f%% density.", density * 100));
  }

  private WorldResult applySeed(OrchestrateRequest req, SimulationGridState state) {
    List<Position> cells = req.cells();
    if (cells.isEmpty()) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "SEED requires at least one position.");
    }
    int planted = 0;
    for (Position pos : cells) {
      int x = (int) pos.getX();
      int y = (int) pos.getY();
      if (!inBounds(x, y)) {
        continue;
      }
      int i = idx(x, y);
      current[i] = TREE;
      burnTimer[i] = 0;
      planted++;
    }
    needsFullSnapshot = true;
    return WorldResult.succeeded("orchestrator", state.worldName(),
        "Planted " + planted + " trees.");
  }

  private WorldResult applyPlace(OrchestrateRequest req, SimulationGridState state) {
    if (req.cells().isEmpty() || req.material() == null) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "PLACE requires a material and position.");
    }
    WildfireCell cell;
    try {
      cell = WildfireCell.valueOf(req.material().toUpperCase());
    } catch (IllegalArgumentException e) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "Unknown cell type: " + req.material());
    }
    Position pos = req.cells().getFirst();
    int x = (int) pos.getX();
    int y = (int) pos.getY();
    if (!inBounds(x, y)) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "Position out of bounds: " + x + "," + y);
    }
    int i = idx(x, y);
    current[i] = cell;
    burnTimer[i] = (cell == BURNING) ? BURN_DURATION : 0;
    needsFullSnapshot = true;
    return WorldResult.succeeded("orchestrator", state.worldName(),
        "Placed " + cell + " at " + x + "," + y);
  }

  private WorldResult applyClear(SimulationGridState state) {
    Arrays.fill(current, EMPTY);
    Arrays.fill(burnTimer, 0);
    windDx = 0f;
    windDy = 0f;
    windSpeed = 0f;
    needsFullSnapshot = true;
    log.info("[Wildfire] CLEAR — grid erased, wind calmed.");
    return WorldResult.succeeded("orchestrator", state.worldName(), "World cleared.");
  }

  private WorldResult applySetWind(OrchestrateRequest req, SimulationGridState state) {
    if (req.material() == null) {
      return WorldResult.failed("orchestrator", state.worldName(),
          "SET_WIND requires direction (material) and speed (density).");
    }
    float[] dir = parseWindDir(req.material().toUpperCase());
    windDx = dir[0];
    windDy = dir[1];
    windSpeed = Math.max(0f, Math.min(1f, req.density()));
    log.info("[Wildfire] Wind set: dir=({},{}) speed={}.", windDx, windDy, windSpeed);
    return WorldResult.succeeded("orchestrator", state.worldName(),
        String.format("Wind %s at speed %.2f.", req.material().toUpperCase(), windSpeed));
  }

  private static float[] parseWindDir(String dir) {
    return switch (dir) {
      case "N" -> new float[]{0f, -1f};
      case "NE" -> new float[]{INV_SQRT2, -INV_SQRT2};
      case "E" -> new float[]{1f, 0f};
      case "SE" -> new float[]{INV_SQRT2, INV_SQRT2};
      case "S" -> new float[]{0f, 1f};
      case "SW" -> new float[]{-INV_SQRT2, INV_SQRT2};
      case "W" -> new float[]{-1f, 0f};
      case "NW" -> new float[]{-INV_SQRT2, -INV_SQRT2};
      default -> {
        log.warn("[Wildfire] Unknown wind direction '{}'; defaulting to calm.", dir);
        yield new float[]{0f, 0f};
      }
    };
  }

  @Override
  public void applyIntents(GridState state) {
    ensureBuffers(state);
    tick();
  }

  private void ensureBuffers(GridState state) {
    int w = state.dimension().getWidth();
    int h = state.dimension().getHeight();
    if (current != null && W == w && H == h) {
      return;
    }
    W = w;
    H = h;
    size = W * H;
    current = new WildfireCell[size];
    next = new WildfireCell[size];
    burnTimer = new int[size];
    nextBurnTimer = new int[size];
    Arrays.fill(current, EMPTY);
    Arrays.fill(next, EMPTY);
    needsFullSnapshot = true;
    log.debug("[WildFire] Allocated {}×{} cell buffers ({} KB).", W, H, size * 5 / 1024);
  }

  @Override
  public String serialize(GridState state) {
    return buildJson(false);
  }

  @Override
  public Object snapshot(GridState state) {
    needsFullSnapshot = false;
    return new WildfireSnapshot(Arrays.copyOf(current, size), W, H, windDx, windDy, windSpeed);
  }

  @Override
  public String serializeOneLine(GridState state) {
    return buildJson(true);
  }

  @Override
  public void reset(GridState state) {
    if (current != null) {
      Arrays.fill(current, EMPTY);
      Arrays.fill(next, EMPTY);
      Arrays.fill(burnTimer, 0);
      Arrays.fill(nextBurnTimer, 0);
    }
    windDx = 0f;
    windDy = 0f;
    windSpeed = 0f;
    needsFullSnapshot = true;
    log.info("[Wildfire] World reset.");
  }

  private String buildJson(boolean oneLine) {
    String sep = oneLine ? "" : "\n";
    String indent = oneLine ? "" : "  ";
    StringBuilder sb = new StringBuilder("{").append(sep);
    sb.append(indent).append("\"tiles\": {").append(sep);
    boolean first = true;
    for (int i = 0; i < size; i++) {
      WildfireCell cell = current[i];
      if (cell == EMPTY) {
        continue;
      }
      if (!first) {
        sb.append(",").append(sep);
      }
      first = false;
      sb.append(indent).append(indent)
          .append('"').append(i % W).append(',').append(i / W).append("\": \"")
          .append(cell.name()).append('"');
    }
    sb.append(sep).append(indent).append("},").append(sep);
    sb.append(indent).append("\"wind\": {")
        .append("\"dx\":").append(windDx)
        .append(",\"dy\":").append(windDy)
        .append(",\"speed\":").append(windSpeed)
        .append("}").append(sep);
    sb.append("}");
    return sb.toString();
  }

  /// Algorithm
  private void tick() {
    clearWriteBuffers();
    System.arraycopy(current, 0, next, 0, bufferSize());
    System.arraycopy(burnTimer, 0, nextBurnTimer, 0, bufferSize());

    for (int y = 0; y < H; y++) {
      for (int x = 0; x < W; x++) {
        int i = idx(x, y);
        if (current[i] == BURNING) {
          nextBurnTimer[i]--;
          if (nextBurnTimer[i] <= 0) {
            next[i] = ASH;
          } else {
            for (int k = 0; k < neighX.length; k++) {
              int nx = x + neighX[k];
              int ny = y + neighY[k];
              int ni = idx(nx, ny);
              if (inBounds(nx, ny) && current[ni] == TREE && next[ni] != BURNING) {
                float prob = BASE_SPREAD_PROBABILITY * windFactor(neighX[k], neighY[k]);
                if (rng.nextFloat() < prob) {
                  next[ni] = BURNING;
                  nextBurnTimer[ni] = BURN_DURATION;
                }
              }
            }
          }
        }
      }
    }

    // Swap buffers — no allocation
    WildfireCell[] tmp = current;
    current = next;
    next = tmp;

    int[] m = burnTimer;
    burnTimer = nextBurnTimer;
    nextBurnTimer = m;


  }

  private float windFactor(int dx, int dy) {
    if (windSpeed == 0) {
      return 1.0f;
    }
    float len = quake3InverseSqrt(dx * dx + dy * dy);
    float sdx = dx / len;
    float sdy = dy / len;
    float dot = sdx * windDx + sdy * windDy;
    return Math.max(0.05f, 1.0f + windSpeed * dot);
  }

  private float quake3InverseSqrt(float x) {
    float xHalf = 0.5f * x;
    int i = Float.floatToIntBits(x);
    i = 0x5f3759df - (i >> 1);
    x = Float.intBitsToFloat(i);
    x = x * (1.5f - xHalf * x * x); // 1st Newton-Raphson iteration
    x = x * (1.5f - xHalf * x * x); // optional 2nd iteration
    return 1 / x;
  }

  private boolean inBounds(int x, int y) {
    return x >= 0 && x < W && y >= 0 && y < H;
  }

  private int idx(int x, int y) {
    return y * W + x;
  }


  /**
   * clearing the write-buffers at the start of each tick
   */
  private void clearWriteBuffers() {
    Arrays.fill(next, EMPTY);
    Arrays.fill(nextBurnTimer, 0);
  }

  private int bufferSize() {
    return W * H;
  }

}
