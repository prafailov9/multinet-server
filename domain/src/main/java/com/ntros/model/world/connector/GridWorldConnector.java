package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.state.solid.GridWorldState;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GridWorldConnector implements WorldConnector {

  private final GridWorldState gridWorldState;
  private final GridWorldEngine gridWorldEngine;

  // NEW: mailbox (ops are produced by session threads, consumed by ticker thread)
  private static final class QueuedOp {

    final WorldOp op;
    final CompletableFuture<CommandResult> promise;

    QueuedOp(WorldOp op) {
      this.op = op;
      this.promise = new CompletableFuture<>();
    }
  }

  private final java.util.concurrent.ConcurrentLinkedQueue<QueuedOp> mailbox =
      new java.util.concurrent.ConcurrentLinkedQueue<>();

  public GridWorldConnector(GridWorldState gridWorldState, GridWorldEngine gridWorldEngine) {
    this.gridWorldState = gridWorldState;
    this.gridWorldEngine = gridWorldEngine;
  }

  // helper: enqueue operation
  private CompletableFuture<CommandResult> submit(WorldOp op) {
    QueuedOp q = new QueuedOp(op);
    mailbox.offer(q);
    return q.promise;
  }

  // ==== WorldConnector impl ====

  @Override
  public void update() {
    // 1) Drain mailbox (single-writer: ticker thread)
    QueuedOp q;
    while ((q = mailbox.poll()) != null) {
      try {
        CommandResult res = q.op.apply(gridWorldEngine, gridWorldState);
        q.promise.complete(res);
      } catch (Throwable t) {
        q.promise.completeExceptionally(t);
      }
    }

    // 2) Advance world (apply stored intents etc)
    gridWorldEngine.applyIntent(gridWorldState);
  }

  // CHANGE: storeMoveIntent now enqueues instead of mutating immediately
  @Override
  public CommandResult storeMoveIntent(MoveRequest move) {
    // If you want immediate ACK and eventual application: do not join here; return a success ACK.
    // If you want to wait for validation on the tick: join() the future.
    // Option A (non-blocking immediate ACK):
    submit((engine, state) -> engine.storeMoveIntent(move, state));
    return new CommandResult(true, move.playerId(), gridWorldState.worldName(), null);

    // Option B (blocking until tick processes it):
    // return submit((engine, state) -> engine.storeMoveIntent(move, state)).join();
  }

  @Override
  public CommandResult add(JoinRequest joinRequest) {
    // Option A: enqueue and immediate ACK
    submit((engine, state) -> engine.add(joinRequest, state));
    return new CommandResult(true, joinRequest.playerName(), gridWorldState.worldName(), null);

    // Option B: block until processed on next tick
    // return submit((engine, state) -> engine.add(joinRequest, state)).join();
  }

  @Override
  public void remove(String entityId) {
    submit((engine, state) -> {
      engine.remove(entityId, state);
      return new CommandResult(true, entityId, state.worldName(), null);
    });
  }

  @Override
  public String serialize() {
    // SAFE: called on the ticker thread right after update() in your instance
    return gridWorldEngine.serialize(gridWorldState);
  }

  @Override
  public String worldName() {
    return gridWorldState.worldName();
  }

  @Override
  public String worldType() {
    return gridWorldState.worldType();
  }

  @Override
  public List<Entity> getCurrentEntities() {
    // Read access: if you want strict single-threading, call this only from ticker.
    return gridWorldState.entities().values().stream().toList();
  }

  @Override
  public void reset() {
    gridWorldEngine.reset(gridWorldState);
    mailbox.clear();
  }
}
