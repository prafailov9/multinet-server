package com.ntros.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomMovementStrategy extends AbstractMovementStrategy {

    private static final ThreadLocalRandom RNG = ThreadLocalRandom.current();
    private static final int ORIGIN = 1;
    private static final int BOUND = Direction.values().length;

    private static List<Direction> DIRECTIONS = initDirectionsList();
    private Direction currentChosenDirection;
    private Position currentChosenPosition;
    private boolean hasIntent;


    @Override
    public boolean hasMovementIntent() {
        return hasIntent;
    }

    @Override
    public Direction getDirection() {
        return intendedDirection;
    }

    @Override
    public Position getPosition() {
        return currentChosenPosition;
    }


    @Override
    public Position decideNextPosition(Position currentPosition) {
        currentChosenDirection = DIRECTIONS.get(RNG.nextInt(ORIGIN, BOUND) - 1);
        DIRECTIONS.remove(currentChosenDirection);

        currentChosenPosition = DirectionUtil.createPosition(currentPosition, currentChosenDirection);
        return currentChosenPosition;
    }

    @Override
    public void confirmChosenDirection() {
        setDirection(currentChosenDirection);
        DIRECTIONS = initDirectionsList();
        hasIntent = true;
    }

    @Override
    public void clearMovementIntent() {
        hasIntent = false;
    }

    private static List<Direction> initDirectionsList() {
        return new ArrayList<>(List.of(Direction.values()));
    }

    private void setCurrentChosenPosition(int x, int y) {
        currentChosenPosition = Position.of(x, y);
    }

}
