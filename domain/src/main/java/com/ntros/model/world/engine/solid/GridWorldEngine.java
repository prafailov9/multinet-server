package com.ntros.model.world.engine.solid;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.Player;
import com.ntros.model.entity.movement.MoveInput;
import com.ntros.model.entity.movement.cell.Position;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.entity.solid.StaticEntity;
import com.ntros.model.world.protocol.TileType;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.state.GridState;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GridWorldEngine implements WorldEngine {

  public GridWorldEngine() {
  }

  @Override
  public void applyIntents(GridState state) {
    for (var stagedIntent : state.moveIntents().entrySet()) {
      Entity entity = state.entities().get(stagedIntent.getKey());
      if (entity == null) {
        log.warn("No entity found for id: {}", stagedIntent.getKey());
        continue;
      }
      Vector4 stagedMoveIntent = stagedIntent.getValue();
      Vector4 currentVec = Vector4.of2dGridPosition(entity.getPosition());
      log.info("Applying move for {}: {} -> {}", entity.getName(), currentVec, stagedMoveIntent);
      moveEntity(entity, currentVec, stagedMoveIntent, state);
    }

    state.moveIntents().clear();
  }

  private Vector4 determineNewPosition(Position current, Position delta) {
    log.info("Determining new position for entity. Current pos: {}, delta: {}", current, delta);

    return Vector4.of(current.getX() + delta.getX(), current.getY() + delta.getY(), 0f, 0f);
  }

  @Override
  public WorldResult storeMoveIntent(MoveRequest moveRequest, GridState state) {
    Entity entity = state.entities().get(moveRequest.playerId());
    MoveInput input = moveRequest.moveInput();
    // 2d space
    Vector4 moveIntent = Vector4.of(input.dx(), input.dy(), input.dz(), input.dw());
    int dx = Math.round(moveIntent.getX());
    int dy = Math.round(moveIntent.getY());
    log.info("received move intent: {}", moveIntent);
    Vector4 newPos = determineNewPosition(entity.getPosition(), Position.of(dx, dy));

    // allow all moves if within bounds
    if (state.isWithinBounds(newPos)) {
      state.moveIntents().put(moveRequest.playerId(), newPos);
      log.info("Added move intent: {}", newPos);
      return WorldResult.succeeded(entity.getName(), state.worldName(), "intent added");
    }
    String msg = String.format("[%s]: invalid move: %s. Out of bounds.", state.worldName(),
        newPos);
    log.info(msg);
    return WorldResult.failed(entity.getName(), state.worldName(), msg);
  }

  @Override
  public WorldResult joinEntity(JoinRequest joinRequest, GridState worldState) {
    // if player exists, return failed result
    if (worldState.entities().containsKey(joinRequest.playerName())) {
      return WorldResult.failed(joinRequest.playerName(), worldState.worldName(),
          String.format("Player with name %s already exists", joinRequest.playerName()));
    }
    long id = IdSequenceGenerator.getInstance().nextPlayerEntityId();
    Vector4 freePosition = findRandomFreePosition(worldState);
    if (freePosition == null) {
      return WorldResult.failed(joinRequest.playerName(), worldState.worldName(),
          "Could not find free position in world.");
    }
    // register player in world
    Player player = new Player(
        Position.of(Math.round(freePosition.getX()), Math.round(freePosition.getY())),
        joinRequest.playerName(), id, 100);
    addEntity(player, worldState);

    // create commandResult
    String successMsg = String.format("Player %s successfully joined world %s with ID: %s",
        player.getName(), worldState.worldName(), id);
    log.info("[GridWorldEngine]: {}", successMsg);

    return WorldResult.succeeded(player.getName(), worldState.worldName(),
        successMsg);
  }

  @Override
  public Entity removeEntity(String entityId, GridState worldState) {
    Entity entity = worldState.entities().remove(entityId);
    worldState.takenPositions()
        .remove(Vector4.of(entity.getPosition().getX(), entity.getPosition().getY(), 0f, 0f));
    return entity;
  }


  @Override
  public String serialize(GridState worldState) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");

    // Terrain first
    sb.append("\"tiles\": {\n");
    int t = 0;
    for (Map.Entry<Vector4, TileType> entry : worldState.terrain().entrySet()) {
      Vector4 p = entry.getKey();
      sb.append(String.format("\t\"%f,%f\": \"%s\"", p.getX(), p.getY(), entry.getValue().name()));
      if (++t < worldState.terrain().size()) {
        sb.append(",\n");
      }
    }
    sb.append("\n},\n");

    // Entities
    sb.append("\"entities\": {\n");
    int e = 0;
    for (Entity entity : worldState.entities().values()) {
      Position pos = entity.getPosition();
      sb.append(String.format("\t\"%s\": {\n\t\t\"x\": %d,\n\t\t\"y\": %d\n\t}", entity.getName(),
          pos.getX(), pos.getY()));
      if (++e < worldState.entities().size()) {
        sb.append(",\n");
      }
    }
    sb.append("\n}\n}");

    return sb.toString();
  }

  @Override
  public String serializeOneLine(GridState worldState) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");

    // Terrain first
    sb.append("\"tiles\": {");
    int t = 0;
    for (Map.Entry<Vector4, TileType> entry : worldState.terrain().entrySet()) {
      Vector4 p = entry.getKey();
      sb.append(String.format("\t\"%f,%f\": \"%s\"", p.getX(), p.getY(), entry.getValue().name()));
      if (++t < worldState.terrain().size()) {
        sb.append(",");
      }
    }
    sb.append("},");

    // Entities
    sb.append("\"entities\": {");
    int e = 0;
    for (Entity entity : worldState.entities().values()) {
      Position pos = entity.getPosition();
      sb.append(
          String.format("\t\"%s\": {\t\t\"x\": %d,\t\t\"y\": %d\t}", entity.getName(), pos.getX(),
              pos.getY()));
      if (++e < worldState.entities().size()) {
        sb.append(",");
      }
    }
    sb.append("}}");

    return sb.toString();
  }

  @Override
  public void reset(GridState gridState) {
    gridState.entities().clear();
    gridState.takenPositions().clear();
    gridState.moveIntents().clear();
    gridState.terrain().clear();
  }

  private void addEntity(StaticEntity entity, GridState worldState) {
    worldState.entities().put(entity.getName(), entity);
    float x = (float) entity.getPosition().getX();
    float y = (float) entity.getPosition().getY();
    worldState.takenPositions().put(Vector4.of(x, y, 0, 0), entity.getName());
    log.info("Added entity: {}", entity);
  }

  /**
   * swap the entities current position(origin) with target in the positions map
   */
  private void moveEntity(Entity entity, Vector4 origin, Vector4 target, GridState worldState) {
    if (worldState.isLegalMove(target)) {
      worldState.takenPositions().remove(origin);
      worldState.takenPositions().put(target, entity.getName());
      entity.setPosition(Position.ofVector4(target));
      log.info("Moved {} from {} to {}.", entity.getName(), origin, target);
    } else {
      log.warn("Failed to move {} to {} position. Illegal move.", entity.getName(), target);
    }
  }

  private Vector4 findRandomFreePosition(GridState state) {
    int width = state.dimension().getWidth();
    int height = state.dimension().getHeight();
    int maxAttempts = width * height; // avoid infinite loops
    while (maxAttempts-- > 0) {
      int x = ThreadLocalRandom.current().nextInt(width);  // 0..width-1
      int y = ThreadLocalRandom.current().nextInt(height); // 0..height-1
      Vector4 candidate = Vector4.of(x, y, 0, 0);

      if (!state.takenPositions().containsKey(candidate)) {
        return candidate;
      }
    }
    return null; // if world is full
  }

}
