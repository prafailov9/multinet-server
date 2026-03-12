package com.ntros.lifecycle.instance;

import com.ntros.codec.packet.StatePacket;
import com.ntros.broadcast.Broadcaster;
import com.ntros.lifecycle.sessionmanager.SessionManager;
import com.ntros.lifecycle.clock.Clock;
import com.ntros.lifecycle.instance.actor.Actor;
import com.ntros.lifecycle.instance.actor.Actors;
import com.ntros.lifecycle.session.Session;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

/**
 * Binds a world to its clients. Unique per world connector + session manager.
 *
 * <h3>Tick flow</h3>
 * <ol>
 *   <li>Clock fires on its scheduler thread → submits task to clock worker.</li>
 *   <li>Clock worker calls {@link #tryWorldUpdate()} → submits step to actor thread via
 *       {@code actor.step(world)} and blocks ({@code .join()}) until the step completes.</li>
 *   <li>Actor thread: {@code applyMoves → world.update() → world.snapshot()} and resolves the
 *       future with the snapshot. Snapshot is taken on the actor thread for consistency.</li>
 *   <li>Clock worker resumes with the snapshot → encodes it as a binary {@link StatePacket} →
 *       {@code broadcaster.publish(frame, ...)} → each session's send queue.</li>
 *   <li>{@code inFlight} clears only after the full cycle (update + broadcast), so the clock
 *       correctly drops ticks when the combined budget is exceeded.</li>
 * </ol>
 */
@Slf4j
public class ServerInstance extends AbstractInstance {

  private final Actor actor;

  public ServerInstance(WorldConnector world, SessionManager sessionManager, Clock clock,
      Broadcaster broadcaster, Settings settings) {
    super(world, sessionManager, clock, broadcaster, settings);
    this.actor = Actors.create(world.getWorldName());
  }

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

  @Override
  public CompletableFuture<Void> drain() {
    if (!actor.isRunning()) {
      return CompletableFuture.completedFuture(null);
    }
    return actor.tell(() -> {
    });
  }

  @Override
  public CompletableFuture<WorldResult> joinAsync(JoinRequest req) {
    // check if clock and actor running
    // if yes: do join
    // if no: check settings.autoStart == true and currentSession.size == 0
    // if yes: do join
    // no: reject join command
    if (isInstanceLive()) {
      return actor.join(world, req);
    }
    if (settings.autoStartOnPlayerJoin() && sessionManager.activeSessionsCount() == 0) {
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
            settings)));
  }

  @Override
  public CompletableFuture<WorldResult> storeMoveAsync(MoveRequest req) {
    return actor.stageMove(world, req);
  }

  @Override
  public CompletableFuture<WorldResult> orchestrateAsync(OrchestrateRequest req) {
    return actor.orchestrate(world, req);
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
   * Runs on the clock worker thread. Blocks until the actor completes the step so that
   * {@code world.snapshot()} is taken on the actor thread — guaranteeing consistency.
   *
   * @return the world snapshot captured by the actor, or {@code null} if the world hasn't started
   */
  private Object tryWorldUpdate() {
    if (!isInstanceLive()) {
      return null;
    }
    try {
      return actor.step(world).join();
    } catch (Throwable t) {
      log.error("[{}] tick failed: {}", getWorldName(), t.getMessage(), t);
      return null;
    }
  }

  /**
   * Encodes the snapshot as a binary {@link StatePacket} and hands the frame to the broadcaster.
   * Runs on the clock worker thread — off the actor, so the actor is free for the next tick.
   */
  private void broadcastWorldUpdate(Object snapshot) {
    if (snapshot == null) {
      return;
    }
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
