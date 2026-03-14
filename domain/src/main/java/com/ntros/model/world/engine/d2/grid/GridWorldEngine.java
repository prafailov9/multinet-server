package com.ntros.model.world.engine.d2.grid;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.Player;
import com.ntros.model.entity.movement.MoveInput;
import com.ntros.model.entity.movement.grid.Position;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.engine.core.PlayerGridEngine;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.EntityView;
import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.core.PlayerGridState;
import com.ntros.model.world.state.grid.CellType;
import com.ntros.model.world.state.grid.GridSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GridWorldEngine implements PlayerGridEngine {

  @Override
  public void applyIntents(GridState state) {
    PlayerGridState pState = (PlayerGridState) state;
    for (var stagedIntent : pState.moveIntents().entrySet()) {
      Entity entity = pState.entities().get(stagedIntent.getKey());
      if (entity == null) {
        log.warn("No entity found for id: {}", stagedIntent.getKey());
        continue;
      }
      Vector4 stagedMoveIntent = stagedIntent.getValue();
      Vector4 currentVec = Vector4.of2dGridPosition(entity.getPosition());
      log.info("Applying move for {}: {} -> {}", entity.getName(), currentVec, stagedMoveIntent);
      moveEntity(entity, currentVec, stagedMoveIntent, pState);
    }
    pState.moveIntents().clear();
  }

  @Override
  public WorldResult joinEntity(JoinRequest req, PlayerGridState state) {
    if (state.entities().containsKey(req.playerName())) {
      return WorldResult.failed(req.playerName(), state.worldName(),
          String.format("Player with name %s already exists", req.playerName()));
    }
    long id = IdSequenceGenerator.getInstance().nextPlayerEntityId();
    Vector4 freePosition = findRandomFreePosition(state);
    if (freePosition == null) {
      return WorldResult.failed(req.playerName(), state.worldName(),
          "Could not find free position in world.");
    }
    Player player = new Player(
        Position.of(Math.round(freePosition.getX()), Math.round(freePosition.getY())),
        req.playerName(), id, 100);
    addEntity(player, state);

    String successMsg = String.format("Player %s successfully joined world %s with ID: %s",
        player.getName(), state.worldName(), id);
    log.info("[GridWorldEngine]: {}", successMsg);
    return WorldResult.succeeded(player.getName(), state.worldName(), successMsg);
  }

  @Override
  public WorldResult storeMoveIntent(MoveRequest req, PlayerGridState state) {
    Entity entity = state.entities().get(req.playerId());
    MoveInput input = req.moveInput();
    Vector4 moveIntent = Vector4.of(input.dx(), input.dy(), input.dz(), input.dw());
    int dx = Math.round(moveIntent.getX());
    int dy = Math.round(moveIntent.getY());
    log.info("received move intent: {}", moveIntent);
    Vector4 newPos = Vector4.of(
        entity.getPosition().getX() + dx,
        entity.getPosition().getY() + dy, 0f, 0f);

    if (state.isWithinBounds(newPos)) {
      state.moveIntents().put(req.playerId(), newPos);
      log.info("Added move intent: {}", newPos);
      return WorldResult.succeeded(entity.getName(), state.worldName(), "intent added");
    }
    String msg = String.format("[%s]: invalid move: %s. Out of bounds.", state.worldName(), newPos);
    log.info(msg);
    return WorldResult.failed(entity.getName(), state.worldName(), msg);
  }

  @Override
  public Entity removeEntity(String entityId, PlayerGridState state) {
    Entity entity = state.entities().remove(entityId);
    state.takenPositions()
        .remove(Vector4.of(entity.getPosition().getX(), entity.getPosition().getY(), 0f, 0f));
    return entity;
  }

  @Override
  public Object snapshot(GridState state) {
    PlayerGridState pState = (PlayerGridState) state;
    return new GridSnapshot(pState.terrain(), buildEntityView(pState));
  }

  @Override
  public String serialize(GridState state) {
    PlayerGridState pState = (PlayerGridState) state;
    StringBuilder sb = new StringBuilder();
    sb.append("{\n\"tiles\": {\n");
    int t = 0;
    for (Map.Entry<Vector4, CellType> entry : pState.terrain().entrySet()) {
      Vector4 p = entry.getKey();
      sb.append(String.format("\t\"%f,%f\": \"%s\"", p.getX(), p.getY(), entry.getValue().name()));
      if (++t < pState.terrain().size()) sb.append(",\n");
    }
    sb.append("\n},\n\"entities\": {\n");
    int e = 0;
    for (Entity entity : pState.entities().values()) {
      Position pos = entity.getPosition();
      sb.append(String.format("\t\"%s\": {\n\t\t\"x\": %d,\n\t\t\"y\": %d\n\t}",
          entity.getName(), pos.getX(), pos.getY()));
      if (++e < pState.entities().size()) sb.append(",\n");
    }
    sb.append("\n}\n}");
    return sb.toString();
  }

  @Override
  public String serializeOneLine(GridState state) {
    PlayerGridState pState = (PlayerGridState) state;
    StringBuilder sb = new StringBuilder("{\"tiles\": {");
    int t = 0;
    for (Map.Entry<Vector4, CellType> entry : pState.terrain().entrySet()) {
      Vector4 p = entry.getKey();
      sb.append(String.format("\t\"%f,%f\": \"%s\"", p.getX(), p.getY(), entry.getValue().name()));
      if (++t < pState.terrain().size()) sb.append(",");
    }
    sb.append("},\"entities\": {");
    int e = 0;
    for (Entity entity : pState.entities().values()) {
      Position pos = entity.getPosition();
      sb.append(String.format("\t\"%s\": {\t\t\"x\": %d,\t\t\"y\": %d\t}",
          entity.getName(), pos.getX(), pos.getY()));
      if (++e < pState.entities().size()) sb.append(",");
    }
    sb.append("}}");
    return sb.toString();
  }

  @Override
  public void reset(GridState state) {
    PlayerGridState pState = (PlayerGridState) state;
    pState.entities().clear();
    pState.takenPositions().clear();
    pState.moveIntents().clear();
    pState.terrain().clear();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private Map<String, EntityView> buildEntityView(PlayerGridState state) {
    Map<String, EntityView> ents = new LinkedHashMap<>();
    state.entities().forEach((name, entity) ->
        ents.put(name, new EntityView(entity.getPosition().getX(), entity.getPosition().getY())));
    return ents;
  }

  private void addEntity(Entity entity, PlayerGridState state) {
    state.entities().put(entity.getName(), entity);
    state.takenPositions()
        .put(Vector4.of2dGridPosition(entity.getPosition()), entity.getName());
    log.info("Added entity: {}", entity);
  }

  private void moveEntity(Entity entity, Vector4 origin, Vector4 target, PlayerGridState state) {
    if (state.isLegalMove(target)) {
      state.takenPositions().remove(origin);
      state.takenPositions().put(target, entity.getName());
      entity.setPosition(Position.ofVector4(target));
      log.info("Moved {} from {} to {}.", entity.getName(), origin, target);
    } else {
      log.warn("Failed to move {} to {} position. Illegal move.", entity.getName(), target);
    }
  }

  private Vector4 findRandomFreePosition(PlayerGridState state) {
    int width = state.dimension().getWidth();
    int height = state.dimension().getHeight();
    int maxAttempts = width * height;
    while (maxAttempts-- > 0) {
      int x = ThreadLocalRandom.current().nextInt(width);
      int y = ThreadLocalRandom.current().nextInt(height);
      Vector4 candidate = Vector4.of(x, y, 0, 0);
      if (!state.takenPositions().containsKey(candidate)) {
        return candidate;
      }
    }
    return null;
  }
}
