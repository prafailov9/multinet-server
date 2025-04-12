package com.ntros.model.world.engine.solid;

import com.ntros.model.entity.Direction;
import com.ntros.model.entity.Entity;
import com.ntros.model.entity.Player;
import com.ntros.model.entity.movement.Position;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.entity.solid.StaticEntity;
import com.ntros.model.world.TileType;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.Result;
import com.ntros.model.world.state.WorldState;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static com.ntros.model.entity.DirectionUtil.createPosition;
import static com.ntros.model.world.utils.LockingUtils.runSafe;

@Slf4j
public class GridWorldEngine implements WorldEngine {

    // locks
    private final ReentrantLock entityMapLock;
    private final ReentrantLock positionMapLock;
    private final ReentrantLock moveIntentsLock;

    public GridWorldEngine() {
        entityMapLock = new ReentrantLock();
        positionMapLock = new ReentrantLock();
        moveIntentsLock = new ReentrantLock();
    }


    @Override
    public void tick(WorldState state) {
        for (Map.Entry<String, Direction> intent : state.moveIntents().entrySet()) {
            StaticEntity entity = state.entities().get(intent.getKey());

            if (entity == null) {
                log.warn("No entity found for id: {}", intent.getKey());
                continue;
            }

            Position currentPosition = entity.getPosition();
            Position newPosition = createPosition(currentPosition, intent.getValue());
            log.info("Processing move for {}: {} -> {}", entity.getName(), currentPosition, newPosition);
            moveEntity(entity, currentPosition, newPosition, state);
        }

        runSafe(state.moveIntents()::clear, moveIntentsLock);
    }

    @Override
    public Result storeMoveIntent(MoveRequest moveRequest, WorldState state) {
        StaticEntity entity = state.entities().get(moveRequest.playerId());
        Position position = createPosition(entity.getPosition(), moveRequest.direction());
        if (state.isWithinBounds(position)) { // allow all moves if within bounds
            runSafe(() -> state.moveIntents().put(moveRequest.playerId(), moveRequest.direction()), moveIntentsLock);
            log.info("Added move intent: {}", moveRequest);
            return new Result(true, entity.getName(), state.worldName(), null);
        }
        String msg = String.format("[%s]: invalid move: %s. Out of bounds.", state.worldName(), position);
        log.info(msg);
        return new Result(false, entity.getName(), state.worldName(), msg);
    }

    @Override
    public Result add(JoinRequest joinRequest, WorldState worldState) {
        long id = IdSequenceGenerator.getInstance().getNextSessionId();
        Position freePosition = findRandomFreePosition(worldState);
        if (freePosition == null) {
            return new Result(false, joinRequest.getPlayerName(), worldState.worldName(), "could not find free position in world.");
        }
        // register player in world
        Player player = new Player(freePosition, joinRequest.getPlayerName(), id, 100);
        addEntity(player, worldState);

        // create result
        Result result = new Result(true, player.getName(), worldState.worldName(), null);
        System.out.printf("[GridWorld]: player: %s joined on position %s%n", player.getName(), player.getPosition());
        return result;
    }

    @Override
    public Entity remove(String entityId, WorldState worldState) {
        StaticEntity entity = runSafe(() -> worldState.entities().remove(entityId), entityMapLock);
        runSafe(() -> worldState.takenPositions().remove(entity.getPosition()), positionMapLock);

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
            if (++t < worldState.terrain().size()) sb.append(",\n");
        }
        sb.append("\n},\n");

        // Entities
        sb.append("\"entities\": {\n");
        int e = 0;
        for (StaticEntity entity : worldState.entities().values()) {
            Position pos = entity.getPosition();
            sb.append(String.format("\t\"%s\": {\n\t\t\"x\": %d,\n\t\t\"y\": %d\n\t}", entity.getName(), pos.getX(), pos.getY()));
            if (++e < worldState.entities().size()) sb.append(",\n");
        }
        sb.append("\n}\n}");

        return sb.toString();
    }

    private void addEntity(StaticEntity entity, WorldState worldState) {
        runSafe(() -> worldState.entities().put(entity.getName(), entity), entityMapLock);
        runSafe(() -> worldState.takenPositions().put(entity.getPosition(), entity.getName()), positionMapLock);
        log.info("Added entity: {0}", entity);
    }

    private void moveEntity(StaticEntity entity, Position origin, Position target, WorldState worldState) {
        if (worldState.isLegalMove(target)) {
            runSafe(() -> {
                worldState.takenPositions().remove(origin);
                worldState.takenPositions().put(target, entity.getName());
                entity.setPosition(target);
            }, positionMapLock);
            log.info("Moved {} from {} to {}.", entity.getName(), origin, target);
        } else {
            log.warn("Failed to move {} to {} position. Illegal move.", entity.getName(), target);
        }
    }

    private Position findRandomFreePosition(WorldState state) {
        int width = state.dimension().getWidth();
        int height = state.dimension().getHeight();

        int maxAttempts = width * height; // avoid infinite loops
        for (int i = 0; i < maxAttempts; i++) {
            int x = IdSequenceGenerator.RNG.nextInt(0, width - 1);
            int y = IdSequenceGenerator.RNG.nextInt(0, height - 1);
            Position candidate = Position.of(x, y);

            if (!state.takenPositions().containsKey(candidate)) {
                return candidate;
            }
        }
        return null; // if world is full
    }

}
