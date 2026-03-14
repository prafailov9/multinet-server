package com.ntros.model.world.state.d2.grid;

public enum CellType {
  EMPTY,
  WALL,
  WATER,
  FIRE,
  TRAP,
  /**
   * A live cell in a Game-of-Life simulation.
   */
  ALIVE,
  /**
   * Additional cells for Falling Sand simulation.
   */
  SAND,
  OIL,
  STONE,
  ACID,
  ASH,
  OBSIDIAN, // unkillable
  SMOKE,
}
