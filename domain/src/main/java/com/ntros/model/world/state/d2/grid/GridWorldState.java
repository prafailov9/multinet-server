package com.ntros.model.world.state.d2.grid;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.dimension.Dimension;
import com.ntros.model.world.state.dimension.Dimension2D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class GridWorldState implements GridState {

  private final Random rng; // NEW


  private final String worldName;
  private final Dimension dimension;
  private final int width;
  private final int height;

  private final Map<String, Entity> entityMap;
  private final Map<Vector4, String> positionMap;
  private final Map<String, Vector4> moveIntentMap;
  private final Map<Vector4, CellType> terrainMap;

  public GridWorldState(String worldName, int width, int height) {
    this(worldName, width, height, ThreadLocalRandom.current()); // default
  }

  public GridWorldState(String worldName, int width, int height, Random rng) {
    this.worldName = worldName;
    this.width = width;
    this.height = height;
    this.rng = Objects.requireNonNull(rng, "rng");
    this.dimension = new Dimension2D(width, height);

    entityMap = new LinkedHashMap<>(); // preserve insertion order
    positionMap = new HashMap<>();
    moveIntentMap = new HashMap<>();
    terrainMap = new HashMap<>();

    generateTerrain();
  }

  /**
   * Creates a blank world state — every cell is {@link CellType#EMPTY}, no walls, no traps.
   *
   * <p>Used for Game-of-Life worlds where the simulation itself controls tile states via
   * {@link CellType#ALIVE}; random terrain generation would interfere with Conway's rules.
   *
   * @param worldName unique world name
   * @param width     number of columns
   * @param height    number of rows
   * @return a fully initialised {@link GridWorldState} with an all-EMPTY terrain map
   */
  public static GridWorldState blank(String worldName, int width, int height) {
    // Sparse representation: no entries means all cells are implicitly EMPTY.
    // Pre-filling 1M EMPTY entries for a 1024×1024 GoL world wastes memory and
    // turns every snapshot/iteration into an O(W×H) operation.
    return new GridWorldState(worldName, width, height, new HashMap<>());
  }

  /**
   * Restoration constructor — uses a pre-built terrain map instead of generating a new one.
   *
   * <p>Used by the persistence layer to recreate stable terrain across server restarts:
   * {@code ServerBootstrap} loads the saved terrain from the snapshot repository and passes it
   * here, so players always see the same map layout.
   *
   * @param worldName    unique world name
   * @param width        number of columns (must match the saved terrain)
   * @param height       number of rows (must match the saved terrain)
   * @param savedTerrain pre-built terrain map loaded from a snapshot; must not be null
   */
  public GridWorldState(String worldName, int width, int height,
      Map<Vector4, CellType> savedTerrain) {
    this.worldName = worldName;
    this.width = width;
    this.height = height;
    this.rng = ThreadLocalRandom.current(); // unused but kept for uniformity
    this.dimension = new Dimension2D(width, height);

    entityMap = new LinkedHashMap<>();
    positionMap = new HashMap<>();
    moveIntentMap = new HashMap<>();
    terrainMap = new HashMap<>(Objects.requireNonNull(savedTerrain, "savedTerrain"));

    log.info("Restored terrain for world '{}' ({} tiles).", worldName, terrainMap.size());
  }


  @Override
  public String worldName() {
    return worldName;
  }

  @Override
  public String worldType() {
    return "GRID";
  }

  @Override
  public Dimension dimension() {
    return dimension;
  }

  @Override
  public Map<String, Entity> entities() {
    return entityMap;
  }

  @Override
  public Map<Vector4, String> takenPositions() {
    return positionMap;
  }

  @Override
  public Map<String, Vector4> moveIntents() {
    return moveIntentMap;
  }

  @Override
  public Map<Vector4, CellType> terrain() {
    return terrainMap;
  }

  @Override
  public CellType getCellTypeAt(Vector4 pos) {
    return terrainMap.get(pos);
  }

  /**
   * Checks if position is occupied. Intents are not concerned with current entity positions
   */
  @Override
  public boolean isLegalMove(Vector4 vector4) {
    return !positionMap.containsKey(vector4)
        && isIntendedMoveValid(vector4); // if position is a tile
  }

  @Override
  public boolean isWithinBounds(Vector4 vec) {
    return (vec.getX() < width && vec.getX() >= 0) && (vec.getY() < height
        && vec.getY() >= 0);
  }

  private boolean isIntendedMoveValid(Vector4 vector4) {
    return isWithinBounds(vector4)
        && terrainMap.getOrDefault(vector4, CellType.EMPTY)
        != CellType.WALL; // if position is a tile
  }

  private void generateTerrain() {
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Vector4 vec = Vector4.of(x, y, 0, 0);
        double r = rng.nextDouble(); // use injected RNG
        CellType tile = r < 0.10 ? CellType.WALL
            : r < 0.15 ? CellType.TRAP
                : r < 0.17 ? CellType.WATER
                    : CellType.EMPTY;
        terrainMap.put(vec, tile);
      }
    }
    log.info("Generated terrain for world: {}", worldName);
  }

}
