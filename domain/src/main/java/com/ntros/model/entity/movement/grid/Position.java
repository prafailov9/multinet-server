package com.ntros.model.entity.movement.grid;

import com.ntros.model.entity.movement.vectors.Vector4;
import java.util.Objects;

public final class Position {

  private final int x;
  private final int y;

  private Position(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public static Position of(int x, int y) {
    return new Position(x, y);
  }

  public static Position ofVector4(Vector4 vector4) {
    return new Position(Math.round(vector4.getX()), Math.round(vector4.getY()));
  }


  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Position position = (Position) o;
    return x == position.x && y == position.y;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  @Override
  public String toString() {
    return "[" + x + ", " + y + "]";
  }

}
