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

  public static Position createPosition(Position currentPosition, int dx, int dy) {
    return Position.of(currentPosition.getX() + dx, currentPosition.getY() + dy);
  }

  public static Position createPosition(Position currentPosition, Position newPosition) {
    return Position.of(currentPosition.getX() + newPosition.getX(), currentPosition.getY() + newPosition.getY());
  }

}
