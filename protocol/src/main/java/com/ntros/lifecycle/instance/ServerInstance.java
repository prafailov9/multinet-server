package com.ntros.lifecycle.instance;

import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.lifecycle.instance.actor.Actor;
import com.ntros.lifecycle.instance.actor.CommandActor;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.protocol.request.RemoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.clock.Clock;
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

  /// --- Constants
  private static final Boolean SERIALIZE_ONE_LINE = Boolean.TRUE;
  private static final int PROTOCOL_VERSION = 1;

  /// --- broadcast pacing + sequence
  private volatile long lastBroadcastNanos = 0L;
  private final long broadcastIntervalNanos; // derived from config.broadcastHz()

  ///  --- Atomic ops
  private final AtomicLong stateSeq = new AtomicLong(0);
  private final AtomicBoolean clockTicking;

  /// --- State
  private final SessionManager sessionManager;
  private final WorldConnector connector;
  private final Clock clock;
  private final Broadcaster broadcaster;
  private final Settings settings;
  private final Actor actor;

  public ServerInstance(WorldConnector connector, SessionManager sessionManager,
      Clock clock, Broadcaster broadcaster, Settings settings) {
    this.connector = connector;
    this.sessionManager = sessionManager;
    this.clock = clock;

    configureTickerListener();

    this.broadcaster = broadcaster;
    this.settings = settings;

    int hz = Math.max(1, settings.broadcastHz()); // guard
    this.broadcastIntervalNanos = 1_000_000_000L / hz;

    this.actor = new CommandActor(true, connector.getWorldName());
    this.clockTicking = new AtomicBoolean(false);
  }

  /**
   * Clock wakes Actor N times per second; Actor advances the world and broadcasts.
   * <p>
   * - Caller Thread(CT) calls instance.run(); * - flips clockTicking flag; calls clock.tick(); -
   * clock.tick() just submits the task on the clock's internal scheduler and returns to
   * instance.run()(still CT). World Update code is NOT run immediately. - At next tick the clock's
   * scheduler thread runs the submitted task. - actor.step() submits its task to its internal
   * executor thread (AT) and returns immediately to the clock scheduler thread(CST). - on AT, the
   * submitted step runs: world update, state frame, publishing and completes the future from step()
   * - During this time, the CT already finished the tick task(since it doesn't wait for Actor's
   * future to complete) and is free to schedule the next tick.
   *
   * </p>
   */
  @Override
  public void run() {
    if (!clockTicking.compareAndSet(false, true)) {
      return;
    }

    log.info("[IN WORLD INSTANCE]: Scheduling update for {} world ...", getWorldName());
    clock.tick(() -> {
      try {
        actor
            .step(connector, () -> {
              if (sessionManager.activeSessionsCount() == 0) {
                return;
              }
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
    if (!clockTicking.get()) {
      return;
    }
    log.info("[{}] resetting…", getWorldName());
    try {
      // 1) stop scheduling new ticks
      clock.stop();
      // 2) ensure all queued actor work (join/move/remove/leave) is drained
      actor.tell(() -> {
      }).join();
      // 3) now we can fully shut down the clock thread
      clock.shutdown();
    } finally {
      clockTicking.set(false);
      sessionManager.shutdownAll();
      connector.reset();
      actor.shutdown();
    }
  }


  /**
   * Schedules a no-op on the actor so the returned future completes after all previously queued
   * tasks (join/move/remove/leave)
   */
  @Override
  public CompletableFuture<Void> drain() {
    return actor.tell(() -> { /* no-op */ });

  }

  @Override
  public void startIfNeededForJoin() {
    if (settings.autoStartOnPlayerJoin() && !isRunning()) {
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
    return actor.stageMove(connector, req);
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
  public void registerSession(Session session) {
    sessionManager.register(session);
    // Auto-start only when configured to do so
    if (settings.autoStartOnPlayerJoin() && !isRunning() && getActiveSessionsCount() > 0) {
      run();
    }
  }

  @Override
  public void removeSession(Session session) {
    sessionManager.remove(session);
    // Auto-stop when last leaves (only for worlds that autostart on join)
    if (settings.autoStartOnPlayerJoin() && isRunning() && getActiveSessionsCount() == 0) {
      reset();
    }
  }

  @Override
  public String getWorldName() {
    return connector.getWorldName();
  }

  @Override
  public Settings getSettings() {
    return settings;
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
