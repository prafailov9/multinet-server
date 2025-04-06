package com.ntros.model.world;

import com.ntros.model.entity.*;
import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.Result;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ntros.model.entity.DirectionUtil.createPosition;
import static com.ntros.model.world.utils.LockingUtils.runSafe;

public class GridWorld implements WorldContext {

    private static final Logger LOGGER = Logger.getLogger(GridWorld.class.getName());

    private final String worldName;
    private final int width;
    private final int height;

    private final Map<String, Entity> entityMap;
    private final Map<Position, String> positionMap;
    private final Map<String, Direction> moveIntentMap;
    private final Map<Position, TileType> terrainMap;

    // locks
    private final ReentrantLock entityMapLock;
    private final ReentrantLock positionMapLock;
    private final ReentrantLock moveIntentsLock;
    private final ReentrantLock terrainMapLock;

    public GridWorld(String worldName, int width, int height) {
        this.worldName = worldName;
        this.width = width;
        this.height = height;

        entityMap = new LinkedHashMap<>(); // preserve insertion order
        positionMap = new HashMap<>();
        moveIntentMap = new HashMap<>();
        terrainMap = new HashMap<>();

        entityMapLock = new ReentrantLock();
        positionMapLock = new ReentrantLock();
        moveIntentsLock = new ReentrantLock();
        terrainMapLock = new ReentrantLock();

        generateTerrain();
    }


    @Override
    public String name() {
        return worldName;
    }

    @Override
    public Result add(JoinRequest joinRequest) {
        long id = IdSequenceGenerator.getInstance().getNextPlayerId();
        Position freePosition = findRandomFreePosition();
        if (freePosition == null) {
            return new Result(false, joinRequest.getPlayerName(), worldName, "could not find free position in world.");
        }
        // register player in world
        Player player = new Player(freePosition, joinRequest.getPlayerName(), id, 100);
        addEntity(player);

        // create result
        Result result = new Result(true, player.getName(), worldName, null);
        System.out.printf("[GridWorld]: player: %s joined on position %s%n", player.getName(), player.getPosition());
        return result;
    }

    @Override
    public void remove(String entityId) {
        Entity entity = runSafe(() -> entityMap.remove(entityId), entityMapLock);
        runSafe(() -> positionMap.remove(entity.getPosition()), positionMapLock);
    }

    @Override
    public void tick() {
        LOGGER.log(Level.INFO, "Executing move intents...");
        LOGGER.log(Level.INFO, "Current state of move intents: {0}", moveIntentMap);

        for (Map.Entry<String, Direction> intent : moveIntentMap.entrySet()) {
            Entity entity = entityMap.get(intent.getKey());

            if (entity == null) {
                LOGGER.warning("No entity found for id: " + intent.getKey());
                continue;
            }

            Position currentPosition = entity.getPosition();
            Position newPosition = createPosition(currentPosition, intent.getValue());

            LOGGER.info("Processing move for " + entity.getName() + ": " + currentPosition + " → " + newPosition);
            moveEntity(entity, currentPosition, newPosition);
        }

        runSafe(moveIntentMap::clear, moveIntentsLock);
    }

    @Override
    public Result storeMoveIntent(MoveRequest moveRequest) {
        // resolve player intent
        Entity entity = entityMap.get(moveRequest.playerId());
        Position position = createPosition(entity.getPosition(), moveRequest.direction());
        if (isWithinBounds(position)) { // allow all moves if within bounds
            runSafe(() -> moveIntentMap.put(moveRequest.playerId(), moveRequest.direction()), moveIntentsLock);
            LOGGER.log(Level.INFO, "Added move intent: {0}", moveRequest);
            return new Result(true, entity.getName(), worldName, null);
        }
        String msg = String.format("[%s]: invalid move: %s. Out of bounds.", worldName, position);
        LOGGER.log(Level.INFO, msg);
        return new Result(false, entity.getName(), worldName, msg);
    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // Terrain first
        sb.append("\"tiles\": {\n");
        int t = 0;
        for (Map.Entry<Position, TileType> entry : terrainMap.entrySet()) {
            Position p = entry.getKey();
            sb.append(String.format("\t\"%d,%d\": \"%s\"", p.getX(), p.getY(), entry.getValue().name()));
            if (++t < terrainMap.size()) sb.append(",\n");
        }
        sb.append("\n},\n");

        // Entities
        sb.append("\"entities\": {\n");
        int e = 0;
        for (Entity entity : entityMap.values()) {
            Position pos = entity.getPosition();
            sb.append(String.format("\t\"%s\": {\n\t\t\"x\": %d,\n\t\t\"y\": %d\n\t}", entity.getName(), pos.getX(), pos.getY()));
            if (++e < entityMap.size()) sb.append(",\n");
        }
        sb.append("\n}\n}");

        return sb.toString();
    }
    

    /**
     * Checks if position is occupied.
     * Intents are not concerned with current entity positions
     */
    @Override
    public boolean isLegalMove(Position position) {
        return !positionMap.containsKey(position)
                && isIntendedMoveValid(position); // if position is a tile
    }

    private boolean isIntendedMoveValid(Position position) {
        return isWithinBounds(position)
                && terrainMap.getOrDefault(position, TileType.EMPTY) != TileType.WALL; // if position is a tile
    }

    private boolean isWithinBounds(Position position) {
        return (position.getX() < width && position.getX() >= 0) && (position.getY() < height && position.getY() >= 0);
    }

    private void moveEntity(Entity entity, Position origin, Position target) {
        LOGGER.info("Current PositionMap Keys: " + positionMap.keySet());
        if (isLegalMove(target)) {
            runSafe(() -> {
                positionMap.remove(origin);
                positionMap.put(target, entity.getName());
                entity.setPosition(target);
            }, positionMapLock);
            LOGGER.info("Moved " + entity.getName() + " from " + origin + " to " + target);
        } else {
            LOGGER.warning("Failed to move " + entity.getName() + " to " + target + " (occupied or out of bounds)");
        }
    }

    private void addEntity(Entity entity) {
        runSafe(() -> entityMap.put(entity.getName(), entity), entityMapLock);
        runSafe(() -> positionMap.put(entity.getPosition(), entity.getName()), positionMapLock);
        LOGGER.log(Level.INFO, "Added entity: {0}", entity);
    }

    private Position findRandomFreePosition() {
        int maxAttempts = width * height; // avoid infinite loops
        for (int i = 0; i < maxAttempts; i++) {
            int x = IdSequenceGenerator.RNG.nextInt(0, width - 1);
            int y = IdSequenceGenerator.RNG.nextInt(0, height - 1);
            Position candidate = Position.of(x, y);

            if (!positionMap.containsKey(candidate)) {
                return candidate;
            }
        }
        return null; // if world is full
    }

    private void generateTerrain() {
        runSafe(() -> {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Position pos = Position.of(x, y);

                    double rand = Math.random();

                    TileType tile = rand < 0.1 ? TileType.WALL :
                            rand < 0.15 ? TileType.TRAP :
                                    rand < 0.17 ? TileType.WATER :
                                            TileType.EMPTY;

                    terrainMap.put(pos, tile);
                }
            }
        }, terrainMapLock);

        LOGGER.info("Generated terrain for world: " + worldName);
    }


}
