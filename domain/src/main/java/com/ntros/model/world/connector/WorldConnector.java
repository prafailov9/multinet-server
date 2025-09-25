package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.protocol.CommandResult;
import java.util.List;

/**
 * Layer, connecting clients to the actual game world. Exposes minimal contract for client
 * interaction with the world. Unique per engine + state.
 */
public interface WorldConnector {

  void update();

  CommandResult storeMoveIntent(MoveRequest move);

  CommandResult add(JoinRequest joinRequest);

  void remove(String entityId);

  String serialize();

  String worldName();

  String worldType();

  List<Entity> getCurrentEntities();

  void reset();
}
