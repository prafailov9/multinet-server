package com.ntros.lifecycle.instance.actor;

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
import com.ntros.lifecycle.session.Session;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CommandActor implements Actor {

  private final ExecutorService control;

  /**
   * last-write-wins move coalescing
   */
  private final ConcurrentHashMap<String, Direction> stagedMoves = new ConcurrentHashMap<>();

  public CommandActor(String worldName) {
    this(true, worldName);
  }

  public CommandActor(boolean runInBackground, String worldName) {
    this.control = Executors.newSingleThreadExecutor(r -> {
      var t = new Thread(r, "actor-" + worldName + "-ctl");
      t.setDaemon(runInBackground);
      return t;
    });
  }

  /**
   * one tick executed entirely on the actor
   *
   * @param world         - state to update
   * @param onAfterUpdate - task to run after updating the state
   * @return void future, signifying the task is completed
   */
  public CompletableFuture<Void> step(WorldConnector world, Runnable onAfterUpdate) {
    // submit world update
    return ask(() -> {
      // flush staged moves to the engine
      if (!stagedMoves.isEmpty()) {
        for (var e : stagedMoves.entrySet()) {
          world.apply(new MoveOp(new MoveRequest(e.getKey(), e.getValue())));
        }
        stagedMoves.clear();
      }
      // advance the world one step: applies flushed intents
      world.update();

      // allow caller to run task on actor thread(snapshot & broadcast)
      onAfterUpdate.run();
      return null;
    });
  }


  @Override
  public CompletableFuture<CommandResult> join(WorldConnector world, JoinRequest join) {
    return ask(() -> world.apply(new JoinOp(join)));
  }

  // coalesce move; do not mutate the world here
  @Override
  public CompletableFuture<CommandResult> stageMove(WorldConnector world, MoveRequest req) {
    stagedMoves.put(req.playerId(), req.direction()); // last-write-wins
    return CompletableFuture.completedFuture(
        CommandResult.succeeded(req.playerId(), world.getWorldName(), "queued"));
  }

  // Used by disconnect
  @Override
  public CompletableFuture<Void> leave(WorldConnector world, SessionManager manager,
      Session session) {

    return ask(() -> {
      var ctx = session.getSessionContext();
      if (ctx.getEntityId() != null) {
        world.apply(new RemoveOp(new RemoveRequest(ctx.getEntityId())));
        stagedMoves.remove(ctx.getEntityId()); // drop any staged move
      }
      manager.remove(session);
      return null;
    });
  }

  @Override
  public CompletableFuture<CommandResult> remove(WorldConnector world, RemoveRequest req) {
    return ask(() -> world.apply(new RemoveOp(req)));
  }

  private <T> CompletableFuture<T> ask(Supplier<T> action) {
    var promise = new CompletableFuture<T>();
    control.execute(() -> {
      try {
        promise.complete(action.get());
      } catch (Throwable t) {
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }

        promise.completeExceptionally(t);
      }
    });
    return promise;
  }

  // Generic run-on-actor hook
  @Override
  public CompletableFuture<Void> tell(Runnable task) {
    return ask(() -> {
      task.run();
      return null;
    });
  }

  @Override
  public boolean isRunning() {
    return !control.isShutdown() && !control.isTerminated();
  }

  @Override
  public void shutdown() {
    if (!isRunning()) {
      log.info("Actor already terminated");
      return;
    }
    control.shutdownNow();
  }
}
