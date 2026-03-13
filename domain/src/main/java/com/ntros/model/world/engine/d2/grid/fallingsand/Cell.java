package com.ntros.model.world.engine.d2.grid.fallingsand;

import com.ntros.model.world.state.d2.grid.CellType;

public class Cell {

  private final CellType cellType;
  private int hp;
  private boolean updated;

  private Cell(CellType cellType) {
    this.cellType = cellType;
  }

  public static Cell of(CellType cellType) {
    return new Cell(cellType);
  }

  ///  ACCESSORS & MUTATORS
  public CellType getCellType() {
    return cellType;
  }

  public int getHp() {
    return hp;
  }

  public boolean isUpdated() {
    return updated;
  }

  public void setHp(int hp) {
    this.hp = hp;
  }

  public void setUpdated(boolean updated) {
    this.updated = updated;
  }


}
