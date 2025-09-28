package com.ntros.model.world.state.solid;

import com.ntros.model.entity.Direction;
import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.Position;
import com.ntros.model.world.protocol.TileType;
import com.ntros.model.world.state.WorldState;
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
public class GridWorldState implements WorldState {

  private final Random rng; // NEW


  private final String worldName;
  private final Dimension dimension;
  private final int width;
  private final int height;

  private final Map<String, Entity> entityMap;
  private final Map<Position, String> positionMap;
  private final Map<String, Direction> moveIntentMap;
  private final Map<Position, TileType> terrainMap;

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
  public Map<Position, String> takenPositions() {
    return positionMap;
  }

  @Override
  public Map<String, Direction> moveIntents() {
    return moveIntentMap;
  }

  @Override
  public Map<Position, TileType> terrain() {
    return terrainMap;
  }

  @Override
  public TileType getTileTypeAt(Position pos) {
    return null;
  }

  /**
   * Checks if position is occupied. Intents are not concerned with current entity positions
   */
  @Override
  public boolean isLegalMove(Position position) {
    return !positionMap.containsKey(position)
        && isIntendedMoveValid(position); // if position is a tile
  }

  @Override
  public boolean isWithinBounds(Position position) {
    return (position.getX() < width && position.getX() >= 0) && (position.getY() < height
        && position.getY() >= 0);
  }

  private boolean isIntendedMoveValid(Position position) {
    return isWithinBounds(position)
        && terrainMap.getOrDefault(position, TileType.EMPTY)
        != TileType.WALL; // if position is a tile
  }

  private void generateTerrain() {
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Position pos = Position.of(x, y);
        double r = rng.nextDouble(); // use injected RNG
        TileType tile = r < 0.10 ? TileType.WALL
            : r < 0.15 ? TileType.TRAP
                : r < 0.17 ? TileType.WATER
                    : TileType.EMPTY;
        terrainMap.put(pos, tile);
      }
    }
    log.info("Generated terrain for world: {}", worldName);
  }

}
