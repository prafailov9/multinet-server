package com.ntros.model.world.engine.solid;

import static com.ntros.model.entity.DirectionUtil.createPosition;
import static com.ntros.model.entity.sequence.IdSequenceGenerator.RNG;
import static com.ntros.model.world.protocol.WorldType.GRID;

import com.ntros.model.entity.Direction;
import com.ntros.model.entity.Entity;
import com.ntros.model.entity.Player;
import com.ntros.model.entity.movement.Position;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.entity.solid.StaticEntity;
import com.ntros.model.world.protocol.TileType;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.state.WorldState;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GridWorldEngine implements WorldEngine {

  public GridWorldEngine() {
  }


  @Override
  public void tick(WorldState state) {
    for (Map.Entry<String, Direction> intent : state.moveIntents().entrySet()) {
      Entity entity = state.entities().get(intent.getKey());

      if (entity == null) {
        log.warn("No entity found for id: {}", intent.getKey());
        continue;
      }

      Position currentPosition = entity.getPosition();
      Position newPosition = createPosition(currentPosition, intent.getValue());
      log.info("Processing move for {}: {} -> {}", entity.getName(), currentPosition, newPosition);
      moveEntity(entity, currentPosition, newPosition, state);
    }

    state.moveIntents().clear();
  }

  @Override
  public CommandResult storeMoveIntent(MoveRequest moveRequest, WorldState state) {
    Entity entity = state.entities().get(moveRequest.playerId());
    Position position = createPosition(entity.getPosition(), moveRequest.direction());
    if (state.isWithinBounds(position)) { // allow all moves if within bounds
      state.moveIntents().put(moveRequest.playerId(), moveRequest.direction());
      log.info("Added move intent: {}", moveRequest);
      return new CommandResult(true, entity.getName(), state.worldName(), null, GRID);
    }
    String msg = String.format("[%s]: invalid move: %s. Out of bounds.", state.worldName(),
        position);
    log.info(msg);
    return new CommandResult(false, entity.getName(), state.worldName(), msg, GRID);
  }

  @Override
  public CommandResult add(JoinRequest joinRequest, WorldState worldState) {
    long id = IdSequenceGenerator.getInstance().getNextSessionId();
    Position freePosition = findRandomFreePosition(worldState);
    if (freePosition == null) {
      return new CommandResult(false, joinRequest.getPlayerName(), worldState.worldName(),
          "could not find free position in world.", GRID);
    }
    // register player in world
    Player player = new Player(freePosition, joinRequest.getPlayerName(), id, 100);
    addEntity(player, worldState);

    // create commandResult
    CommandResult commandResult = new CommandResult(true, player.getName(),
        worldState.worldName(), null, GRID);
    System.out.printf("[GridWorld]: player: %s joined World %s on position %s%n", player.getName(),
        worldState.worldName(), player.getPosition());
    return commandResult;
  }

  @Override
  public Entity remove(String entityId, WorldState worldState) {
    Entity entity = worldState.entities().remove(entityId);
    worldState.takenPositions().remove(entity.getPosition());
    return entity;
  }


  @Override
  public String serialize(WorldState worldState) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");

    // Terrain first
    sb.append("\"tiles\": {\n");
    int t = 0;
    for (Map.Entry<Position, TileType> entry : worldState.terrain().entrySet()) {
      Position p = entry.getKey();
      sb.append(String.format("\t\"%d,%d\": \"%s\"", p.getX(), p.getY(), entry.getValue().name()));
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
  public String serializeOneLine(WorldState worldState) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");

    // Terrain first
    sb.append("\"tiles\": {");
    int t = 0;
    for (Map.Entry<Position, TileType> entry : worldState.terrain().entrySet()) {
      Position p = entry.getKey();
      sb.append(String.format("\t\"%d,%d\": \"%s\"", p.getX(), p.getY(), entry.getValue().name()));
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
  public void reset(WorldState worldState) {
    worldState.entities().clear();
    worldState.takenPositions().clear();
    worldState.moveIntents().clear();
    worldState.terrain().clear();
  }

  private void addEntity(StaticEntity entity, WorldState worldState) {
    worldState.entities().put(entity.getName(), entity);
    worldState.takenPositions().put(entity.getPosition(), entity.getName());
    log.info("Added entity: {}", entity);
  }

  private void moveEntity(Entity entity, Position origin, Position target, WorldState worldState) {
    if (worldState.isLegalMove(target)) {
      worldState.takenPositions().remove(origin);
      worldState.takenPositions().put(target, entity.getName());
      entity.setPosition(target);
      log.info("Moved {} from {} to {}.", entity.getName(), origin, target);
    } else {
      log.warn("Failed to move {} to {} position. Illegal move.", entity.getName(), target);
    }
  }

  private Position findRandomFreePosition(WorldState state) {
    int width = state.dimension().getWidth();
    int height = state.dimension().getHeight();
    int maxAttempts = width * height; // avoid infinite loops
    while (maxAttempts-- > 0) {
      int x = RNG.nextInt(0, width - 1);
      int y = RNG.nextInt(0, height - 1);
      Position candidate = Position.of(x, y);

      if (!state.takenPositions().containsKey(candidate)) {
        return candidate;
      }
    }
    return null; // if world is full
  }

}
