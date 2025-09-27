package com.ntros.instance.actor;

import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.message.SessionContext;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorldInstanceActor implements InstanceActor {

  private final ExecutorService control;

  public WorldInstanceActor(boolean runInBackground, String worldName) {
    this.control = Executors.newSingleThreadExecutor(r -> {
      var t = new Thread(r, "inst-" + worldName + "-ctl");
      t.setDaemon(runInBackground);
      return t;
    });
  }

  @Override
  public CompletableFuture<CommandResult> join(WorldConnector worldConnector,
      JoinRequest joinRequest) {
    var futureResult = new CompletableFuture<CommandResult>();
    control.execute(() -> {
      try {
        futureResult.complete(worldConnector.apply(new JoinOp(joinRequest)));
      } catch (Throwable t) {
        futureResult.completeExceptionally(t);
      }
    });
    return futureResult;
  }


  @Override
  public CompletableFuture<CommandResult> move(WorldConnector worldConnector, MoveRequest req) {
    var futureResult = new CompletableFuture<CommandResult>();
    control.execute(() -> {
      try {
        // Optional: coalesce here by storing last intent per entity in a map and applying later
        var result = worldConnector.apply(new MoveOp(req));
        futureResult.complete(result);
      } catch (Throwable t) {
        futureResult.completeExceptionally(t);
      }
    });
    return futureResult;
  }

  @Override
  public CompletableFuture<CommandResult> remove(WorldConnector worldConnector, RemoveRequest req) {
    var futureResult = new CompletableFuture<CommandResult>();
    control.execute(() -> {
      try {
        futureResult.complete(worldConnector.apply(new RemoveOp(req)));
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
  public CompletableFuture<Void> execute(Runnable action) {
    var fut = new CompletableFuture<Void>();
    control.execute(() -> {
      try {
        action.run();
        fut.complete(null);
      } catch (Throwable t) {
        fut.completeExceptionally(t);
      }
    });
    return fut;
  }

  @Override
  public CompletableFuture<Void> leave(WorldConnector worldConnector, SessionManager sessionManager,
      Session session) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    control.execute(() -> {
      try {
        // remove entity if present
        String entityId = session.getSessionContext().getEntityId();
        ;
        if (entityId != null && !entityId.isBlank()) {
          worldConnector.apply(new RemoveOp(new RemoveRequest(entityId)));
        }
        // deregister session (stops it from receiving further STATE)
        sessionManager.remove(session);
        future.complete(null);
      } catch (Throwable t) {
        future.completeExceptionally(t);
      }
    });
    return future;
  }

  @Override
  public void stopActor() {
    control.shutdownNow();
  }


}
