package com.ntros.instance.ins;

import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.instance.actor.Actor;
import com.ntros.instance.actor.CommandActor;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.protocol.request.RemoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.session.Session;
import com.ntros.ticker.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * Layer that allows the world to interact with clients. Unique per world connector + session
 * manager.
 */

@Slf4j
public class ServerInstance implements Instance {

  private static final Boolean SERIALIZE_ONE_LINE = Boolean.TRUE;
  private static final int PROTOCOL_VERSION = 1;

  // NEW: broadcast pacing + sequence
  private volatile long lastBroadcastNanos = 0L;
  private final long broadcastIntervalNanos; // derived from config.broadcastHz()
  private final AtomicLong stateSeq = new AtomicLong(0);

  private final SessionManager sessionManager;
  private final WorldConnector connector;
  private final Clock clock;
  private final Broadcaster broadcaster;
  private final Settings config;
  private final AtomicBoolean tickerRunning;
  private final Actor actor;

  public ServerInstance(WorldConnector connector, SessionManager sessionManager,
      Clock clock, Broadcaster broadcaster, Settings config) {
    this.connector = connector;
    this.sessionManager = sessionManager;
    this.clock = clock;

    configureTickerListener();

    this.broadcaster = broadcaster;
    this.config = config;

    int hz = Math.max(1, config.broadcastHz()); // guard
    this.broadcastIntervalNanos = 1_000_000_000L / hz;

    this.actor = new CommandActor(true, connector.getWorldName());
    this.tickerRunning = new AtomicBoolean(false);
  }

  /**
   * Clock wakes WorldThread N times per second; WorldThread advances the world and broadcasts.
   */
  @Override
  public void run() {
    if (!tickerRunning.compareAndSet(false, true)) {
      return;
    }

    log.info("[IN WORLD INSTANCE]: Updating {} state...", getWorldName());
    clock.tick(() -> {
      try {
        actor
            .step(connector, () -> {
              // Still on actor thread here; safe to snapshot & broadcast
              String frame = buildStateFrame();
              broadcaster.publish(frame, sessionManager);
            })
            .exceptionally(ex -> {
              log.error("[{}] tick failed: {}", getWorldName(), ex.toString());
              return null;
            });
      } catch (Throwable t) {
        log.error("[{}] scheduling tick failed: {}", getWorldName(), t.toString());
      }
    });

    tickerRunning.set(true);
  }

  private String buildStateFrame() {
    long seq = stateSeq.incrementAndGet();
    String snap = connector.snapshot(SERIALIZE_ONE_LINE);
    // Prefix carries protocol and sequencing info (easy to parse/ignore)
    // e.g.: STATE proto=1 inst=arena-x seq=42 <json>
    // Produce: STATE {"proto":1,"inst":"arena-x","seq":42,"data":{...}}
    return "STATE " +
        "{\"proto\":" + PROTOCOL_VERSION +
        ",\"inst\":\"" + getWorldName() + "\"" +
        ",\"seq\":" + seq +
        ",\"data\":" + snap + "}";
  }

  private void rateLimitedBroadcast() {
    long now = System.nanoTime();
    if (now - lastBroadcastNanos >= broadcastIntervalNanos) {
      lastBroadcastNanos = now;
      String frame = buildStateFrame();
      broadcaster.publish(frame, sessionManager);
    }
  }

  private void standardBroadcast() {
    // includes proto/inst/seq
    String frame = buildStateFrame();
    // broadcast to clients
    broadcaster.publish(frame, sessionManager);
  }

  @Override
  public void reset() {
    if (!tickerRunning.get()) {
      return;
    }
    log.info("[{}] resetting…", getWorldName());
    try {
      clock.shutdown();           // stop ticks first (no more STATE)
      actor.execute(() -> {
          }) // ensure control queue drained
          .join();
    } finally {
      tickerRunning.set(false);
      sessionManager.shutdownAll();
      connector.reset();
      actor.stopActor();
    }
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
  public Settings getConfig() {
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
    clock.pause();
  }

  @Override
  public void resume() {
    clock.resume();
  }

  @Override
  public void updateTickRate(int tps) {
    clock.updateTickRate(tps);
  }

  private String getWorldSnapshot() {
    return SERIALIZE_ONE_LINE
        ? String.format("STATE %s", connector.snapshot(true))
        : String.format("STATE \n%s\n", connector.snapshot(false));
  }

  private void configureTickerListener() {
    this.clock.setListener(new Clock.TickListener() {
      @Override
      public void onTickStart(long n) { /* noop */ }

      @Override
      public void onTickEnd(long n, long nanos) {
        if (n % 120 == 0) { // sample
          log.debug("[{}] tick={} duration={}µs", getWorldName(), n, nanos / 1_000);
        }
      }
    });
  }


}
