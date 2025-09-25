package com.ntros.instance;

import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.listener.SessionManager;
import com.ntros.instance.ins.Instance;
import com.ntros.model.entity.config.access.WorldConfig;
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
  private final WorldConnector worldConnector;
  private final Ticker ticker;
  private final Broadcaster broadcaster;
  private final AtomicBoolean tickerRunning;

  public WorldInstance(WorldConnector worldConnector, SessionManager sessionManager,
      Ticker ticker, Broadcaster broadcaster) {
    this.worldConnector = worldConnector;
    this.sessionManager = sessionManager;
    this.ticker = ticker;
    this.broadcaster = broadcaster;
    this.tickerRunning = new AtomicBoolean(false);
  }

  @Override
  public String worldName() {
    return worldConnector.worldName();
  }

  @Override
  public void run() {
    log.info("[IN WORLD INSTANCE]: Updating {} state...", worldName());
    ticker.tick(() -> {
      try {
        worldConnector.update();                // mutate on ticker thread

        final String serialized = worldConnector.serialize(); // snapshot on ticker thread

        // Optional: avoid megaspam logs / huge payload logs
        log.debug("[{}] broadcasting state ({} bytes)", worldName(), serialized.length());

        broadcaster.publish(serialized, sessionManager);
      } catch (Throwable t) {
        log.error("[{}] tick failed: {}", worldName(), t.toString(), t);
      }
    });
    tickerRunning.set(true);
  }

  @Override
  public WorldConfig getWorldPolicy() {
    return null;
  }

  @Override
  public void reset() {
    if (!tickerRunning.get()) {
      return;
    }
    log.info("Resetting World {}...", worldName());
    ticker.shutdown();
    tickerRunning.set(false);
    try {
      sessionManager.shutdownAll();
    } finally {
      worldConnector.reset();
    }
  }


  @Override
  public void registerSession(Session session) {
    sessionManager.register(session);
  }

  @Override
  public void removeSession(Session session) {
    sessionManager.remove(session);
  }

  @Override
  public Session getSession(Long sessionId) {
    return sessionManager.getActiveSessions()
        .stream()
        .filter(session -> session.getProtocolContext().getSessionId() == sessionId)
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
