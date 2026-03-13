package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.protocol.result.WorldResult;
import java.util.List;

/**
 * Layer, connecting clients to the actual game world. Exposes minimal contract for client
 * interaction with the world. Unique per engine + state.
 */
public interface WorldConnector {

  // Synchronous, immediate mutations:
  WorldResult apply(WorldOp op);

  void update();

  Object snapshot();

  String snapshot(boolean oneLine);

  String getWorldName();

  String getWorldType();

  List<Entity> getCurrentEntities();

  WorldCapabilities getCapabilities();

  void reset();
}
