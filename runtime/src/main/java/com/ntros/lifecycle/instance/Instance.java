package com.ntros.lifecycle.instance;

import com.ntros.lifecycle.Lifecycle;
import com.ntros.lifecycle.Pausable;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.lifecycle.session.Session;
import java.util.concurrent.CompletableFuture;

public interface Instance extends Lifecycle, Pausable {
  /**
   * Marker task that makes sure all enqueued tasks before it are finished.
   */
  CompletableFuture<Void> drain(); // runs after all queued actor tasks
  void startIfNeededForJoin();   // safe to call anytime
  // --- Domain actions exposed to commands ---
  CompletableFuture<WorldResult> joinAsync(JoinRequest req);
  CompletableFuture<WorldResult> storeMoveAsync(MoveRequest req);
  CompletableFuture<WorldResult> orchestrateAsync(OrchestrateRequest req);
  CompletableFuture<Void> leaveAsync(Session session);
  WorldConnector getWorldConnector();
  Settings getSettings();
  String getWorldName();
  // --- Session lifecycle (called from response pipeline) ---
  void registerSession(Session session);
  void removeSession(Session session);
  int getActiveSessionsCount();
  int getEntityCount();
  void updateTickRate(int ticksPerSecond);
}
