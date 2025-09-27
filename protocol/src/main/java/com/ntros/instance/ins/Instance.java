package com.ntros.instance.ins;

import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.session.Session;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Instance {


  void startIfNeededForJoin();   // safe to call anytime

  // --- Domain actions exposed to commands ---
  CompletableFuture<CommandResult> joinAsync(JoinRequest req);

  CommandResult move(MoveRequest req);        // immediate ACK

  void removeEntity(String entityId);         // fire-and-forget

  // --- Session lifecycle (called from response pipeline) ---
  void onWelcomeSent(Session session);        // register AFTER WELCOME was sent

  void run();

  InstanceConfig getConfig();

  WorldConnector getWorldConnector();

  String getWorldName();

  void reset();

  void registerSession(Session session);

  void removeSession(Session session);

  Session getSession(Long sessionId);

  int getActiveSessionsCount();

  int getEntityCount();

  boolean isRunning();

  void pause();

  void resume();

  void updateTickRate(int ticksPerSecond);

  // TODO: Refactor worldStateUpdates Map to store Generic world types.
  void updateWorldState(Map<Boolean, Boolean> worldStateUpdates);

}
