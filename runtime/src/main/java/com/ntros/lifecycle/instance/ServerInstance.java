package com.ntros.lifecycle.instance;

import com.ntros.event.broadcaster.Broadcaster;
import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.instance.actor.Actor;
import com.ntros.lifecycle.instance.actor.Actors;
import com.ntros.lifecycle.session.Session;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.protocol.encoder.StateFrame;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

/**
 * Layer that allows the world to interact with clients. Unique per world connector + session
 * manager.
 */

@Slf4j
public class ServerInstance extends AbstractInstance {

  /// --- Constants
  private static final int PROTO_VER = 1;
  private final Actor actor;

  public ServerInstance(WorldConnector world, SessionManager sessionManager, Clock clock,
      Broadcaster broadcaster, Settings settings) {
    super(world, sessionManager, clock, broadcaster, settings);

    this.actor = Actors.create(world.getWorldName());
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
    if (tryTicking()) {
      return;
    }
    clock.tick(() -> {
      Object snapshot = tryWorldUpdate();
      broadcastWorldUpdate(snapshot);
    });
  }


  /**
   * Schedules a no-op on the actor so the returned future completes after all previously queued
   * tasks (join/move/remove/leave)
   */
  @Override
  public CompletableFuture<Void> drain() {
    if (!actor.isRunning()) {
      return CompletableFuture.completedFuture(null);
    }
    return actor.tell(() -> {
    });
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
    if (!actor.isRunning()) {
      sessionManager.remove(session);
      return CompletableFuture.completedFuture(null);
    }
    return actor.leave(world, sessionManager, session);
  }

  @Override
  public void stop() {

    if (!clockTicking.get()) {
      return;
    }

    log.info("[{}] resetting…", getWorldName());

    try {

      // 1 stop scheduling new ticks
      clock.stop();
      // 2 finish all actor work
      drain().join();
      // 3 shutdown sessions (may enqueue actor tasks)
      sessionManager.shutdownAll();
      // 4 drain again to process leave() operations
      drain().join();
      // 5 reset world state
      world.reset();
    } finally {
      clockTicking.set(false);
      // 6 stop actor
      actor.shutdown();
      // 7 stop clock thread
      clock.shutdown();
    }
  }

  private Object tryWorldUpdate() {
    Object snapshot = null;
    if (!hasWorldStarted()) {
      return null;
    }
    try {
      snapshot = actor.step(world).join();
    } catch (Throwable t) {
      log.error("Tick failed", t);
    }
    return snapshot;
  }

  /**
   * TODO: replace with current broadcast
   * private void broadcastWorldUpdate(Object snapshot) {
   *     byte[] body  = encoder.encodeBody(snapshot);                       // JsonProtocolEncoder
   *     StatePacket packet = new StatePacket(seq.incrementAndGet(), getWorldName(), body);
   *     byte[] frame;
   *     try {
   *         frame = codec.encode(packet);                                  // PacketCodec
   *     } catch (IOException e) {
   *         log.error("state encode failed", e);
   *         return;
   *     }
   *     broadcaster.publish(frame, sessionManager);                        // needs publish(byte[], ...)
   * }
   */

  private void broadcastWorldUpdate(Object snapshot) {
    String dataLine = encoder.encodeState(
        new StateFrame(PROTO_VER, getWorldName(), seq.incrementAndGet(), snapshot));
    broadcaster.publish(dataLine, sessionManager);
  }

  private boolean tryTicking() {
    return !clockTicking.compareAndSet(false, true);
  }

  private boolean hasWorldStarted() {
    return clockTicking.get() && actor.isRunning();
  }

  @Override
  protected void configureClockListener() {
    this.clock.setListener(new Clock.TickListener() {
      @Override
      public void onTickStart(long n) {
        /* noop */
        log.info("ON TICK START: tickCount={}", n);
      }

      @Override
      public void onTickEnd(long n, long nanos) {
        if (n % 120 == 0) { // sample
          log.info("ON TICK END: [{}] tickCount={} duration={}µs", getWorldName(), n,
              nanos / 1_000);
        }
      }
    });
  }
}
