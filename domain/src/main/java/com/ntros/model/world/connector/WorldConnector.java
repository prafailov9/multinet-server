package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.CommandResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Layer, connecting clients to the actual game world. Exposes minimal contract for client
 * interaction with the world. Unique per engine + state.
 */
public interface WorldConnector {

  void update();

  CommandResult storeMoveIntent(MoveRequest move);

  CommandResult joinPlayer(JoinRequest joinRequest);

  CompletableFuture<CommandResult> joinPlayerAsynch(JoinRequest joinRequest);

  void removePlayer(String entityId);

  String snapshot(boolean oneLine);

  String getWorldName();

  String getWorldType();

  List<Entity> getCurrentEntities();

  WorldCapabilities getCapabilities();

  void reset();
}
