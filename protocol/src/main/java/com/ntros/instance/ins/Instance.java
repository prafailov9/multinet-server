package com.ntros.instance.ins;

import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.session.Session;
import java.util.concurrent.CompletableFuture;

public interface Instance {


  /**
   * Marker task that makes sure all enqueued tasks before it are finished.
   */
  CompletableFuture<Void> drainControl(); // runs after all queued actor tasks

  void startIfNeededForJoin();   // safe to call anytime

  CommandResult joinSync(JoinRequest req);

  // --- Domain actions exposed to commands ---
  CompletableFuture<CommandResult> joinAsync(JoinRequest req);

  CompletableFuture<CommandResult> storeMoveAsync(MoveRequest req);

  CompletableFuture<Void> leaveAsync(Session session);

  CompletableFuture<CommandResult> removeEntityAsync(String entityId);

  void removeEntity(String entityId);         // fire-and-forget

  // --- Session lifecycle (called from response pipeline) ---

  void run();

  Settings getSettings();

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

}
