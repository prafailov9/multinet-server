package com.ntros.instance.actor;

import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.model.entity.Direction;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.connector.ops.MoveOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.request.RemoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.session.Session;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CommandActor implements Actor {

  private final ExecutorService control;

  // last-write-wins move coalescing
  private final ConcurrentHashMap<String, Direction> stagedMoves = new ConcurrentHashMap<>();

  public CommandActor(boolean runInBackground, String worldName) {
    this.control = Executors.newSingleThreadExecutor(r -> {
      var t = new Thread(r, "ins-" + worldName + "-ctl");
      t.setDaemon(runInBackground);
      return t;
    });
  }

  // one tick executed entirely on the actor
  public CompletableFuture<Void> step(WorldConnector world,
      Runnable onAfterUpdate) {
    return execute(() -> {
      // 1) flush staged moves into engine intents
      if (!stagedMoves.isEmpty()) {
        for (var e : stagedMoves.entrySet()) {
          world.apply(new MoveOp(new MoveRequest(e.getKey(), e.getValue())));
        }
        stagedMoves.clear();
      }
      // 2) advance world one step
      world.update();

      // 3) allow caller to snapshot & broadcast (still on actor)
      onAfterUpdate.run();
    });
  }

  // Generic "run on actor" hook
  @Override
  public CompletableFuture<Void> execute(Runnable task) {
    var p = new CompletableFuture<Void>();
    control.execute(() -> {
      try {
        task.run();
        p.complete(null);
      } catch (Throwable t) {
        p.completeExceptionally(t);
      }
    });
    return p;
  }

  @Override
  public CompletableFuture<CommandResult> join(WorldConnector world, JoinRequest join) {
    var p = new CompletableFuture<CommandResult>();
    control.execute(() -> {
      try {
        p.complete(world.apply(new JoinOp(join)));
      } catch (Throwable t) {
        p.completeExceptionally(t);
      }
    });
    return p;
  }

  // CHANGE: coalesce move; do not mutate the world here
  @Override
  public CompletableFuture<CommandResult> move(WorldConnector world, MoveRequest req) {
    stagedMoves.put(req.playerId(), req.direction()); // last-write-wins
    return CompletableFuture.completedFuture(
        CommandResult.succeeded(req.playerId(), world.getWorldName(), "queued"));
  }

  // Used by disconnect
  @Override
  public CompletableFuture<Void> leave(WorldConnector world,
      SessionManager manager,
      Session session) {
    return execute(() -> {
      // remove entity + deregister in actor order
      var ctx = session.getSessionContext();
      if (ctx.getEntityId() != null) {
        world.apply(new RemoveOp(new RemoveRequest(ctx.getEntityId())));
        stagedMoves.remove(ctx.getEntityId()); // drop any staged move
      }
      manager.remove(session);
    });
  }

  @Override
  public CompletableFuture<CommandResult> remove(WorldConnector world, RemoveRequest req) {
    var futureResult = new CompletableFuture<CommandResult>();
    control.execute(() -> {
      try {
        futureResult.complete(world.apply(new RemoveOp(req)));
      } catch (Throwable t) {
        futureResult.completeExceptionally(t);
      }
    });
    return futureResult;
  }

  @Override
  public boolean isRunning() {
    return control.isTerminated();
  }

  @Override
  public void stopActor() {
    if (!isRunning()) {
      log.info("Actor already terminated");
      return;
    }
    control.shutdownNow();
  }
}
