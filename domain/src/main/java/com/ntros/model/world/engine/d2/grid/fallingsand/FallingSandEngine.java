package com.ntros.model.world.engine.d2.grid.fallingsand;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.engine.d2.grid.AbstractGridEngine;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.state.core.GridState;
import com.ntros.model.world.state.d2.grid.CellType;
import java.util.Map;

public class FallingSandEngine extends AbstractGridEngine {


  // gravity affected
  // current
  private CellType[] current;
  private CellType[] next;
  private int[] metadata;  // fire TTL, acid strength, etc. — parallel array

  private int W, H;

  @Override
  public void applyIntents(GridState state) {

  }

  @Override
  public WorldResult storeMoveIntent(MoveRequest move, GridState state) {
    return null;
  }

  @Override
  public WorldResult joinEntity(JoinRequest joinRequest, GridState state) {
    return null;
  }

  @Override
  public Entity removeEntity(String entityId, GridState state) {
    return null;
  }

  @Override
  public String serialize(GridState state) {
    return "";
  }

  @Override
  public String serializeOneLine(GridState state) {
    return "";
  }

  @Override
  public void reset(GridState state) {
  }

  private void nextGeneration(GridState state) {
    Map<Vector4, CellType> terrain = state.terrain();
    final int W = state.dimension().getWidth();
    final int H = state.dimension().getHeight();


  }

  /**
   * Sand is affected by gravity.
   * Sand action priority:
   * - try moving downward
   * - if cell below is empty -> sand occupies that cell.
   * - if cell below contains a lower-density material(water/oil) -> sand swaps pos with that mat
   * <p>
   * - if sand cannot move downward:
   * - attempts to move on downward diagonals: down-left and/or down-right
   * - if either cell is empty or contains ld material -> move there
   * When both directions are possible - the dir is chosen randomly
   * If non of these movements are possible -> sand remains in place
   */
  private void applySandRules(Map<Vector4, CellType> terrain) {

  }

}
