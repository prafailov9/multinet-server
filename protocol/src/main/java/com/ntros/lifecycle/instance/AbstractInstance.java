package com.ntros.lifecycle.instance;

import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.instance.actor.Actor;
import com.ntros.lifecycle.instance.actor.Actors;
import com.ntros.lifecycle.session.Session;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.encoder.JsonProtocolEncoder;
import com.ntros.model.world.protocol.encoder.ProtocolEncoder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractInstance implements Instance {

  /// --- Counters + flags
  // sequence number of each queried world state
  protected final AtomicLong seq = new AtomicLong(0);
  protected final AtomicBoolean clockTicking;

  /// --- broadcast pacing + sequence
  protected volatile long lastBroadcastNanos = 0L;
  protected final long broadcastIntervalNanos; // derived from config.broadcastHz()

  ///  State
  protected final WorldConnector world;
  protected final SessionManager sessionManager;
  protected final Settings settings;
  protected final Clock clock;
  protected final Broadcaster broadcaster;
  protected final ProtocolEncoder encoder;

  AbstractInstance(WorldConnector world, SessionManager sessionManager, Clock clock,
      Broadcaster broadcaster, Settings settings) {
    this.world = world;
    this.sessionManager = sessionManager;

    this.clock = clock;
    this.clockTicking = new AtomicBoolean(false);
    configureClockListener();

    this.broadcaster = broadcaster;
    this.settings = settings;

    this.broadcastIntervalNanos = toNanos();

    this.encoder = new JsonProtocolEncoder();
  }

  @Override
  public void registerSession(Session session) {
    sessionManager.register(session);
    // Auto-start only when configured to do so
    if (settings.autoStartOnPlayerJoin() && !isRunning() && getActiveSessionsCount() > 0) {
      start();
    }
  }

  @Override
  public void removeSession(Session session) {
    sessionManager.remove(session);
    // Auto-stop when last leaves (only for worlds that autostart on join)
    if (settings.autoStartOnPlayerJoin() && isRunning() && getActiveSessionsCount() == 0) {
      this.stop();
    }
  }

  @Override
  public Settings getSettings() {
    return settings;
  }

  @Override
  public void startIfNeededForJoin() {
    if (settings.autoStartOnPlayerJoin() && !isRunning()) {
      start();
    }
  }

  @Override
  public int getActiveSessionsCount() {
    return sessionManager.activeSessionsCount();
  }

  @Override
  public String getWorldName() {
    return world.getWorldName();
  }

  @Override
  public WorldConnector getWorldConnector() {
    return world;
  }

  @Override
  public int getEntityCount() {
    return world.getCurrentEntities().size();
  }

  @Override
  public boolean isRunning() {
    return clockTicking.get();
  }

  @Override
  public void pause() {
    clock.pause();
  }

  @Override
  public void resume() {
    clock.resume();
  }

  @Override
  public boolean isPaused() {
    return clock.isPaused();
  }

  @Override
  public void updateTickRate(int tps) {
    clock.updateTickRate(tps);
  }

  protected abstract void configureClockListener();

  private long toNanos() {
    return 1_000_000_000L / Math.max(1, settings.broadcastHz());// guard
  }

}
