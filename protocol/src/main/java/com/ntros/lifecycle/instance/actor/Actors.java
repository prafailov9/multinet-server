package com.ntros.lifecycle.instance.actor;

import com.ntros.lifecycle.instance.actor.movestrategy.ApplyMoveStrategy;
import com.ntros.lifecycle.instance.actor.movestrategy.StageMovesStrategy;

public final class Actors {

  public static CommandActor create(String worldName, boolean stageMoves) {
    return new CommandActor(worldName,
        stageMoves ? new StageMovesStrategy() : new ApplyMoveStrategy());
  }

}
