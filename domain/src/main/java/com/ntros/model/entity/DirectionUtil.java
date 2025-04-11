package com.ntros.model.entity;

import com.ntros.model.entity.movement.Position;

public class DirectionUtil {

    public static Position createPosition(Position currentPosition, Direction direction) {
        switch (direction) {
            case RIGHT -> {
                return Position.of(currentPosition.getX() + 1, currentPosition.getY());
            }
            case LEFT -> {
                return Position.of(currentPosition.getX() - 1, currentPosition.getY());
            }
            case UP -> {
                return Position.of(currentPosition.getX(), currentPosition.getY() + 1);
            }
            case DOWN -> {
                return Position.of(currentPosition.getX(), currentPosition.getY() - 1);
            }
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        }
    }

}
