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
import com.ntros.model.world.protocol.encoder.StateFrame;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;
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
  private static final int PROTO_VER = 1;

  /// --- broadcast pacing + sequence
  private volatile long lastBroadcastNanos = 0L;
  private final long broadcastIntervalNanos; // derived from config.broadcastHz()

  /// --- Counters + flags
  // sequence number of each queried world state
  private final AtomicLong seq = new AtomicLong(0);
  private final AtomicBoolean clockTicking;

  /// --- State
  private final SessionManager sessionManager;
  private final WorldConnector world;
  private final Clock clock;
  private final Broadcaster broadcaster;
  private final Settings settings;
  private final Actor actor;
  private final ProtocolEncoder encoder;

  public ServerInstance(WorldConnector world, SessionManager sessionManager, Clock clock,
      Broadcaster broadcaster, Settings settings) {
    this.world = world;
    this.sessionManager = sessionManager;
    this.clock = clock;

    configureClockListener();

    this.broadcaster = broadcaster;
    this.settings = settings;

    int hz = Math.max(1, settings.broadcastHz()); // guard
    this.broadcastIntervalNanos = 1_000_000_000L / hz;
    this.actor = Actors.create(world.getWorldName(), settings.stageMoves());
    this.clockTicking = new AtomicBoolean(false);
    this.encoder = new JsonProtocolEncoder();
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
  public void start() {
    if (!clockTicking.compareAndSet(false, true)) {
      return;
    }

    clock.tick(() -> {
      try {
        actor.step(world, () -> {
          // on actor thread, safe to snapshot
          Object data = world.snapshot();
          String dataLine = encoder.encodeState(
              new StateFrame(PROTO_VER, getWorldName(), seq.incrementAndGet(), data));
          broadcaster.publish(dataLine, sessionManager);
        }).exceptionally(ex -> {
          log.error("tick failed", ex);
          return null;
        });
      } catch (Throwable t) {
        log.error("scheduling tick failed", t);
      }
    });

    clockTicking.set(true);
  }

  @Override
  public void stop() {
    if (!clockTicking.get()) {
      return;
    }
    log.info("[{}] resetting…", getWorldName());
    try {
      // 1) stop scheduling new ticks
      clock.stop();
      // 2) ensure all queued actor work (join/move/remove/leave) is drained
      drain().join();
      // 3) now we can fully shut down the clock thread
      clock.shutdown();
    } finally {
      clockTicking.set(false);
      sessionManager.shutdownAll();
      world.reset();
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
      start();
    }
  }

  @Override
  public CompletableFuture<CommandResult> joinAsync(JoinRequest req) {
    return actor.join(world, req);
  }

  @Override
  public CompletableFuture<CommandResult> storeMoveAsync(MoveRequest req) {
    return actor.stageMove(world, req);
  }

  @Override
  public CompletableFuture<Void> leaveAsync(Session session) {
    return actor.leave(world, sessionManager, session);
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
  public String getWorldName() {
    return world.getWorldName();
  }

  @Override
  public Settings getSettings() {
    return settings;
  }

  @Override
  public WorldConnector getWorldConnector() {
    return world;
  }

  @Override
  public int getActiveSessionsCount() {
    return sessionManager.activeSessionsCount();
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

  private String getWorldSnapshot() {
    return SERIALIZE_ONE_LINE ? String.format("STATE %s", world.snapshot(true))
        : String.format("STATE \n%s\n", world.snapshot(false));
  }

  private void configureClockListener() {
    this.clock.setListener(new Clock.TickListener() {
      @Override
      public void onTickStart(long n) {
        /* noop */
//        log.info("ON TICK START: tickCount={}", n);
      }

      @Override
      public void onTickEnd(long n, long nanos) {
//        if (n % 120 == 0) { // sample
//        log.info("ON TICK END: [{}] tickCount={} duration={}µs", getWorldName(), n,
//            nanos / 1_000);
//        }
      }
    });
  }
}
