package com.ntros.instance.ins;

import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.instance.actor.InstanceActor;
import com.ntros.instance.actor.WorldInstanceActor;
import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.protocol.request.RemoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.session.Session;
import com.ntros.ticker.Ticker;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Layer that allows the world to interact with clients. Unique per world connector + session
 * manager.
 */

@Slf4j
public class WorldInstance implements Instance {

  private static final Boolean SERIALIZE_ONE_LINE = Boolean.TRUE;

  private final SessionManager sessionManager;
  private final WorldConnector connector;
  private final Ticker ticker;
  private final Broadcaster broadcaster;
  private final InstanceConfig config;
  private final AtomicBoolean tickerRunning;
  private final InstanceActor actor;

  public WorldInstance(WorldConnector connector, SessionManager sessionManager,
      Ticker ticker, Broadcaster broadcaster, InstanceConfig config) {
    this.connector = connector;
    this.sessionManager = sessionManager;
    this.ticker = ticker;
    this.broadcaster = broadcaster;
    this.config = config;

    this.actor = new WorldInstanceActor(true, connector.getWorldName());
    this.tickerRunning = new AtomicBoolean(false);
  }


  /**
   * Schedules a no-op on the actor so the returned future completes after all previously queued
   * tasks (join/move/remove/leave)
   */
  @Override
  public CompletableFuture<Void> drainControl() {
    return actor.execute(() -> { /* no-op */ });

  }

  @Override
  public void startIfNeededForJoin() {
    if (config.autoStartOnPlayerJoin() && !isRunning()) {
      run();
    }
  }

  @Override
  public CommandResult joinSync(JoinRequest req) {
    return connector.apply(new JoinOp(req));
  }


  @Override
  public CompletableFuture<CommandResult> joinAsync(JoinRequest req) {
    return actor.join(connector, req);
  }

  @Override
  public CompletableFuture<CommandResult> storeMoveAsync(MoveRequest req) {
    return actor.move(connector, req);
  }

  @Override
  public CompletableFuture<Void> leaveAsync(Session session) {
    return actor.leave(connector, sessionManager, session);
  }

  @Override
  public CompletableFuture<CommandResult> removeEntityAsync(String entityId) {
    return actor.remove(connector, new RemoveRequest(entityId));
  }

  @Override
  public void removeEntity(String entityId) {
    actor.remove(connector, new RemoveRequest(entityId));
  }

  @Override
  public void run() {
    if (!tickerRunning.compareAndSet(false, true)) {
      return;
    }

    log.info("[IN WORLD INSTANCE]: Updating {} state...", getWorldName());
    ticker.tick(() -> {
      try {
        // mutate and get world state on ticker thread
        connector.update();
        String protocolFormatSnapshot = getWorldSnapshot();

        // broadcast to clients
        broadcaster.publish(protocolFormatSnapshot, sessionManager);
      } catch (Throwable t) {
        log.error("[{}] tick failed: {}", getWorldName(), t);
      }
    });
    tickerRunning.set(true);
  }

  @Override
  public void reset() {
    log.info("[{}] resetting…", getWorldName());
    try {
      // Stop ticking first so no new STATE gets scheduled
      ticker.shutdown();
    } finally {
      tickerRunning.set(false);
      try {
        // Drop all sessions (no further STATE will be sent)
        sessionManager.shutdownAll();
      } finally {
        try {
          connector.reset();
        } finally {
          // IMPORTANT: stop actor thread last so any pending leave/remove tasks can run
          actor.stopActor();
        }
      }
    }
  }

  @Override
  public void registerSession(Session session) {
    sessionManager.register(session);
    // Auto-start only when configured to do so
    if (config.autoStartOnPlayerJoin() && !isRunning() && getActiveSessionsCount() > 0) {
      run();
    }
  }

  @Override
  public void removeSession(Session session) {
    sessionManager.remove(session);
    // Auto-stop when last leaves (only for worlds that autostart on join)
    if (config.autoStartOnPlayerJoin() && isRunning() && getActiveSessionsCount() == 0) {
      reset();
    }
  }

  @Override
  public String getWorldName() {
    return connector.getWorldName();
  }

  @Override
  public InstanceConfig getConfig() {
    return config;
  }

  @Override
  public WorldConnector getWorldConnector() {
    return connector;
  }

  @Override
  public Session getSession(Long sessionId) {
    return sessionManager.getActiveSessions()
        .stream()
        .filter(session -> session.getSessionContext().getSessionId() == sessionId)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            String.format("Session with ID:[%s] does not exist", sessionId)));
  }

  @Override
  public int getActiveSessionsCount() {
    return sessionManager.activeSessionsCount();
  }

  @Override
  public int getEntityCount() {
    return connector.getCurrentEntities().size();
  }

  @Override
  public boolean isRunning() {
    return tickerRunning.get();
  }

  @Override
  public void pause() {
    ticker.pause();
  }

  @Override
  public void resume() {
    ticker.resume();
  }

  @Override
  public void updateTickRate(int tps) {
    ticker.updateTickRate(tps);
  }

  private String getWorldSnapshot() {
    return SERIALIZE_ONE_LINE
        ? String.format("STATE %s", connector.snapshot(true))
        : String.format("STATE \n%s\n", connector.snapshot(false));
  }

}
