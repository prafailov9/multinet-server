package com.ntros.instance.ins;

import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.listener.SessionManager;
import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.session.Session;
import com.ntros.ticker.Ticker;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Layer that allows the world to interact with clients. Unique per world connector + session
 * manager.
 */

@Slf4j
public class WorldInstance implements Instance {

  private final SessionManager sessionManager;
  private final WorldConnector world;
  private final Ticker ticker;
  private final Broadcaster broadcaster;
  private final InstanceConfig config;
  private final AtomicBoolean tickerRunning;

  public WorldInstance(WorldConnector world, SessionManager sessionManager,
      Ticker ticker, Broadcaster broadcaster, InstanceConfig config) {
    this.world = world;
    this.sessionManager = sessionManager;
    this.ticker = ticker;
    this.broadcaster = broadcaster;
    this.config = config;
    this.tickerRunning = new AtomicBoolean(false);
  }


  @Override
  public void run() {
    if (!tickerRunning.compareAndSet(false, true)) {
      return;
    }

    log.info("[IN WORLD INSTANCE]: Updating {} state...", getWorldName());
    ticker.tick(() -> {
      try {
        world.update();                // mutate on ticker thread

        final String serialized = world.serialize(); // snapshot on ticker thread

        // Optional: avoid megaspam logs / huge payload logs
        log.debug("[{}] broadcasting state ({} bytes)", getWorldName(), serialized.length());

        broadcaster.publish(serialized, sessionManager);
      } catch (Throwable t) {
        log.error("[{}] tick failed: {}", getWorldName(), t);
      }
    });
    tickerRunning.set(true);
  }

  @Override
  public void reset() {
    if (!tickerRunning.get()) {
      return;
    }
    log.info("[{}] resetting…", getWorldName());
    try {
      ticker.shutdown();
    } finally {
      tickerRunning.set(false);
      sessionManager.shutdownAll();
      world.reset();
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
    return world.worldName();
  }

  @Override
  public InstanceConfig getConfig() {
    return config;
  }

  @Override
  public WorldConnector getWorldConnector() {
    return world;
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

  @Override
  public void updateWorldState(Map<Boolean, Boolean> worldStateUpdates) {

  }

}
