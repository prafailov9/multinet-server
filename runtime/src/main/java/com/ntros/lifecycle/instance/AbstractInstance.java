package com.ntros.lifecycle.instance;

import com.ntros.codec.PacketCodec;
import com.ntros.broadcast.Broadcaster;
import com.ntros.lifecycle.sessionmanager.SessionManager;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.session.Session;
import com.ntros.model.entity.config.LifecyclePolicy;
import com.ntros.model.entity.config.access.InstanceSettings;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.protocol.encoder.JsonProtocolEncoder;
import com.ntros.protocol.encoder.ProtocolEncoder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractInstance implements Instance {

  // ── Counters + flags ──────────────────────────────────────────────────────
  protected final AtomicLong seq = new AtomicLong(0);
  protected final AtomicBoolean clockTicking;

  // ── Broadcast pacing ──────────────────────────────────────────────────────
  protected volatile long lastBroadcastNanos = 0L;
  protected final long broadcastIntervalNanos; // derived from instanceSettings.broadcastHz()

  // ── Core components ───────────────────────────────────────────────────────
  protected final WorldConnector world;
  protected final SessionManager sessionManager;
  protected final InstanceSettings instanceSettings;
  protected final Clock clock;
  protected final Broadcaster broadcaster;
  protected final ProtocolEncoder encoder;

  /** Shared, stateless codec — safe to share across all instances. */
  protected static final PacketCodec CODEC = new PacketCodec();

  AbstractInstance(WorldConnector world, SessionManager sessionManager, Clock clock,
      Broadcaster broadcaster, InstanceSettings instanceSettings) {
    this.world = world;
    this.sessionManager = sessionManager;
    this.clock = clock;
    this.clockTicking = new AtomicBoolean(false);
    configureClockListener();
    this.broadcaster = broadcaster;
    this.instanceSettings = instanceSettings;
    this.broadcastIntervalNanos = toNanos();
    this.encoder = new JsonProtocolEncoder();
  }

  @Override
  public void registerSession(Session session) {
    sessionManager.register(session);
    // Auto-start only for PLAYER_DRIVEN worlds when the first player arrives.
    if (isPlayerDriven() && !isRunning() && getActiveSessionsCount() > 0) {
      start();
    }
  }

  @Override
  public void removeSession(Session session) {
    sessionManager.remove(session);
    // Auto-stop when the last player leaves (PLAYER_DRIVEN worlds only).
    if (isPlayerDriven() && isRunning() && getActiveSessionsCount() == 0) {
      stop();
    }
  }

  @Override
  public InstanceSettings getSettings() {
    return instanceSettings;
  }

  @Override
  public void startIfNeededForJoin() {
    if (isPlayerDriven() && !isRunning()) {
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

  // ── Internal ──────────────────────────────────────────────────────────────

  protected abstract void configureClockListener();

  /**
   * Returns {@code true} when this world's lifecycle is {@link LifecyclePolicy#PLAYER_DRIVEN}.
   * PLAYER_DRIVEN worlds auto-start on first join and auto-stop when all players leave.
   */
  protected boolean isPlayerDriven() {
    return world.getCapabilities().lifecyclePolicy() == LifecyclePolicy.PLAYER_DRIVEN;
  }

  protected boolean isOrchestrationDriven() {
    return world.getCapabilities().lifecyclePolicy() == LifecyclePolicy.ORCHESTRATION_DRIVEN;
  }

  private long toNanos() {
    return 1_000_000_000L / Math.max(1, instanceSettings.broadcastHz());
  }
}
