package com.ntros.lifecycle.instance;

import com.ntros.codec.packet.StatePacket;
import com.ntros.broadcast.Broadcaster;
import com.ntros.lifecycle.sessionmanager.SessionManager;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.instance.actor.Actor;
import com.ntros.lifecycle.instance.actor.Actors;
import com.ntros.lifecycle.session.Session;
import com.ntros.model.entity.config.access.InstanceSettings;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

/**
 * Binds a world to its clients. Unique per world connector + session manager.
 */
@Slf4j
public class ServerInstance extends AbstractInstance {

  private final Actor actor;

  public ServerInstance(WorldConnector world, SessionManager sessionManager, Clock clock,
      Broadcaster broadcaster, InstanceSettings instanceSettings) {
    super(world, sessionManager, clock, broadcaster, instanceSettings);
    this.actor = Actors.create(world.getWorldName());
  }

  @Override
  public void start() {
    if (tryTicking()) {
      log.info("[ServerInstance]: Instance already started");
      return;
    }
    clock.tick(() -> {
      // Decide broadcast eligibility before touching the actor so we can tell it
      // whether to take a snapshot (expensive) or just advance the simulation.
      boolean broadcast = shouldBroadcastNow();
      Object snapshot = tryWorldUpdate(broadcast);
      if (snapshot != null) {
        broadcastWorldUpdate(snapshot);
      }
    });
  }

  /**
   * Returns true and advances {@link #lastBroadcastNanos} when the configured broadcast
   * interval has elapsed.  Called once per clock tick, on the clock-worker thread.
   */
  private boolean shouldBroadcastNow() {
    long now = System.nanoTime();
    if (now - lastBroadcastNanos >= broadcastIntervalNanos) {
      lastBroadcastNanos = now;
      return true;
    }
    return false;
  }

  @Override
  public CompletableFuture<Void> drain() {
    if (!actor.isRunning()) {
      return CompletableFuture.completedFuture(null);
    }
    return actor.tell(() -> {
    });
  }

  /**
   * TODO: Stop mixing access control with simple actor delegation. Accessing an instance should be an upstream concern. Possibly a InstanceAdmissionController is needed
   */
  @Override
  public CompletableFuture<WorldResult> joinAsync(JoinRequest req) {
    // check if clock and actor running
    // - yes: do join
    // - no: check settings.autoStart == true and currentSession.size == 0
    //       - yes: do join
    //       - no: reject join command
    if (isInstanceLive()) {
      return actor.join(world, req);
    }
    if (instanceSettings.autoStartOnPlayerJoin() && sessionManager.activeSessionsCount() == 0) {
      // Submit the join task FIRST so it is at the head of the actor queue,
      // then start the clock. This guarantees [joinTask → stepTask] ordering in
      // the actor's single-threaded executor: the entity is added before the first
      // step snapshot is taken, even when initialDelay=0 fires immediately.
      CompletableFuture<WorldResult> joinFuture = actor.join(world, req);
      start();
      return joinFuture;
    }
    return CompletableFuture.completedFuture(WorldResult.failed(req.playerName(), getWorldName(),
        String.format(
            " JOIN required instance start. Not allowed base on settings.autoJoin=false. Instance settings: %s",
            instanceSettings)));
  }

  @Override
  public CompletableFuture<WorldResult> orchestrateAsync(OrchestrateRequest req) {
    // valid req if instance is orchestrated
    if (!instanceSettings.requiresOrchestrator()) {
      return CompletableFuture.completedFuture(
          WorldResult.failed(req.action().name(), getWorldName(),
              String.format(
                  " ORCHESTRATE requires  an orchestrator instance. Current instance settings don't allow orchestration. Instance settings: %s",
                  instanceSettings)));
    }

    if (!isInstanceLive()) {
      // Submit orchestrate first so it lands at the head of the actor queue (world gets seeded
      // before the first step snapshot is taken), then start the clock — same ordering guarantee
      // as joinAsync.
      CompletableFuture<WorldResult> future = actor.orchestrate(world, req);
      start();
      return future;
    }

    return actor.orchestrate(world, req);
  }

  @Override
  public CompletableFuture<WorldResult> storeMoveAsync(MoveRequest req) {
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
      clock.stop();               // 1. stop scheduling new ticks
      drain().join();             // 2. finish in-flight actor work
      sessionManager.shutdownAll(); // 3. shutdown sessions (may enqueue leave tasks)
      drain().join();             // 4. drain leave operations
      world.reset();              // 5. wipe world state
    } finally {
      clockTicking.set(false);
      actor.shutdown();           // 6. stop actor executor
      clock.shutdown();           // 7. stop clock threads
    }
  }

  /**
   * Advances the world simulation on the actor thread.
   *
   * <p>When {@code takeSnapshot} is {@code true} the actor runs a full step (update + snapshot)
   * and the snapshot object is returned for broadcasting.  When {@code false} only the world
   * update is performed; no snapshot is taken and {@code null} is returned.  This avoids
   * computing and serialising a diff for every tick when the broadcast interval has not yet
   * elapsed, keeping the engine's {@code broadcastBasis} aligned with ticks that actually
   * produce a frame.
   *
   * @param takeSnapshot whether a snapshot should be taken this tick
   * @return the world snapshot, or {@code null} if the world hasn't started or no snapshot taken
   */
  private Object tryWorldUpdate(boolean takeSnapshot) {
    if (!isInstanceLive()) {
      return null;
    }
    try {
      if (takeSnapshot) {
        return actor.step(world).join(); // update + snapshot
      }
      actor.step(world, () -> {
      }).join(); // update only — no snapshot
      return null;
    } catch (Throwable t) {
      log.error("[{}] tick failed: {}", getWorldName(), t.getMessage(), t);
      return null;
    }
  }

  /**
   * Encodes the snapshot as a binary {@link StatePacket} and hands the frame to the broadcaster.
   * Runs on the clock worker thread — off the actor, so the actor is free for the next tick.
   * TODO: broadcasting details should be delegated to a broadcaster or a ReplicationService.
   */
  private void broadcastWorldUpdate(Object snapshot) {
    byte[] body = encoder.encodeBody(snapshot);
    try {
      byte[] frame = CODEC.encode(new StatePacket(seq.incrementAndGet(), getWorldName(), body));
      broadcaster.publish(frame, sessionManager);
    } catch (IOException e) {
      log.error("[{}] broadcastWorldUpdate encode failed: {}", getWorldName(), e.getMessage());
    }
  }

  private boolean tryTicking() {
    return !clockTicking.compareAndSet(false, true);
  }

  private boolean isInstanceLive() {
    return clockTicking.get() && actor.isRunning();
  }

  @Override
  protected void configureClockListener() {
    this.clock.setListener(new Clock.TickListener() {
      @Override
      public void onTickStart(long n) { /* noop */ }

      @Override
      public void onTickEnd(long n, long nanos) {
        if (n % 120 == 0) {
          log.info("[{}] tick={} duration={}µs", getWorldName(), n, nanos / 1_000);
        }
      }
    });
  }
}
