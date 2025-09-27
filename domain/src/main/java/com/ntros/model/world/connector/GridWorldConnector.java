package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.connector.ops.MoveOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.model.world.state.solid.GridWorldState;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GridWorldConnector implements WorldConnector {

  private final GridWorldState state;
  private final GridWorldEngine engine;
  private final WorldCapabilities worldCapabilities;

  // NEW: mailbox (ops are produced by session threads, consumed by ticker thread)
  private static final class QueuedOp {

    final WorldOp op;
    final CompletableFuture<CommandResult> promise;

    QueuedOp(WorldOp op) {
      this.op = op;
      this.promise = new CompletableFuture<>();
    }
  }

  private final ConcurrentLinkedQueue<QueuedOp> mailbox = new ConcurrentLinkedQueue<>();

  public GridWorldConnector(GridWorldState state, GridWorldEngine engine,
      WorldCapabilities worldCapabilities) {
    this.state = state;
    this.engine = engine;
    this.worldCapabilities = worldCapabilities;
  }

  // helper: enqueue operation
  private void submit(WorldOp op) {
    QueuedOp q = new QueuedOp(op);
    mailbox.offer(q);
  }

  // return the promise so callers can await "actually applied"
  private CompletableFuture<CommandResult> submitAsync(WorldOp op) {
    QueuedOp q = new QueuedOp(op);
    mailbox.offer(q);
    return q.promise;
  }

  // --- Apply submitted commands
  @Override
  public void update() {
    // apply pending ops
    QueuedOp q;
    while ((q = mailbox.poll()) != null) {
      try {
        q.promise.complete(apply(q.op));
      } catch (Throwable t) {
        q.promise.completeExceptionally(t);
      }
    }
    // world tick
    engine.applyIntents(state);
  }

  private CommandResult apply(WorldOp op) {
    return switch (op) {
      case JoinOp j -> engine.joinEntity(j.req(), state);
      case MoveOp m -> engine.storeMoveIntent(m.req(), state);
      case RemoveOp r -> {
        engine.removeEntity(r.entityId(), state);
        yield CommandResult.succeeded(r.entityId(), state.worldName(), "success");
      }
    };
  }

  // --- Write commands to the world ---
  @Override
  public CommandResult storeMoveIntent(MoveRequest move) {
    submit(new MoveOp(move)); // enqueue; applied on next tick
    return new CommandResult(true, move.playerId(), state.worldName(), null); // immediate ACK
  }

  @Override
  public CommandResult joinPlayer(JoinRequest joinRequest) {
    submit(new JoinOp(joinRequest)); // enqueue; applied on next tick
    return new CommandResult(true, joinRequest.playerName(), state.worldName(),
        null); // immediate ACK
  }

  @Override
  public CompletableFuture<CommandResult> joinPlayerAsynch(JoinRequest joinRequest) {
    return submitAsync(new JoinOp(joinRequest));
  }

  @Override
  public void removePlayer(String entityId) {
    submit(new RemoveOp(entityId)); // enqueue
  }

  @Override
  public String snapshot(boolean oneLine) {
    // SAFE: called on the ticker thread right after update() in your instance
    return oneLine ? engine.serializeOneLine(state)
        : engine.serialize(state);
  }

  @Override
  public String getWorldName() {
    return state.worldName();
  }

  @Override
  public String getWorldType() {
    return state.worldType();
  }

  @Override
  public List<Entity> getCurrentEntities() {
    // Read access: if you want strict single-threading, call this only from ticker.
    return state.entities().values().stream().toList();
  }

  @Override
  public WorldCapabilities getCapabilities() {
    return worldCapabilities;
  }

  @Override
  public void reset() {
    engine.reset(state);
    mailbox.clear();
  }
  /// TODO: APPLY THESE CHANGES AFTER FIXING TESTS

  //public class GridWorldConnector implements WorldConnector {
  //
  //  private static final int MAX_CONTROL_OPS = 4096; // backpressure for control queue
  //
  //  private final GridWorldState state;
  //  private final GridWorldEngine engine;
  //  private final WorldCapabilities worldCapabilities;
  //
  //  // --- Control ops (JOIN/REMOVE/etc.) with promises ---
  //  private static final class QueuedOp {
  //    final WorldOp op;
  //    final CompletableFuture<CommandResult> promise;
  //    QueuedOp(WorldOp op) {
  //      this.op = op;
  //      this.promise = new CompletableFuture<>();
  //    }
  //  }
  //
  //  // FIFO for control ops
  //  private final ConcurrentLinkedQueue<QueuedOp> controlQueue = new ConcurrentLinkedQueue<>();
  //
  //  // Coalesced MOVE intents (latest per entityId)
  //  private final ConcurrentHashMap<String, MoveRequest> moveIntents = new ConcurrentHashMap<>();
  //
  //  private void submitControl(WorldOp op) {
  //    // simple bound; you can reserve capacity for critical ops if you like
  //    if (controlQueue.size() >= MAX_CONTROL_OPS) {
  //      // drop or throw depending on your policy; here we drop with log
  //      // log.warn("Control mailbox full; dropping {}", op);
  //      return;
  //    }
  //    controlQueue.offer(new QueuedOp(op));
  //  }
  //
  //  private CompletableFuture<CommandResult> submitControlAsync(WorldOp op) {
  //    if (controlQueue.size() >= MAX_CONTROL_OPS) {
  //      var failed = new CompletableFuture<CommandResult>();
  //      failed.completeExceptionally(new IllegalStateException("WORLD_BUSY"));
  //      return failed;
  //    }
  //    var q = new QueuedOp(op);
  //    controlQueue.offer(q);
  //    return q.promise;
  //  }
  //
  //  @Override
  //  public void update() {
  //    // 1) Drain and apply control ops (JOIN/REMOVE/etc.) in FIFO order
  //    QueuedOp q;
  //    while ((q = controlQueue.poll()) != null) {
  //      try {
  //        q.promise.complete(applyControl(q.op));
  //      } catch (Throwable t) {
  //        q.promise.completeExceptionally(t);
  //      }
  //    }
  //
  //    // 2) Snapshot and clear current move intents (coalesced)
  //    if (!moveIntents.isEmpty()) {
  //      var moves = new ArrayList<MoveRequest>(moveIntents.size());
  //      moveIntents.forEach((id, req) -> moves.add(req));
  //      moveIntents.clear();
  //
  //      // Apply the latest intent for each entity
  //      for (MoveRequest mr : moves) {
  //        engine.storeMoveIntent(mr, state);
  //      }
  //    }
  //
  //    // 3) Advance world one tick
  //    engine.applyIntents(state);
  //  }
  //
  //  private CommandResult applyControl(WorldOp op) {
  //    if (op instanceof JoinOp j) {
  //      return engine.joinEntity(j.req(), state);
  //    } else if (op instanceof RemoveOp r) {
  //      engine.removeEntity(r.entityId(), state);
  //      return CommandResult.succeeded(r.entityId(), state.worldName(), "removed");
  //    }
  //    // MOVE never goes here now; it's coalesced separately
  //    throw new IllegalStateException("Control op not recognized: " + op);
  //  }
  //
  //  // ---------- Public API ----------
  //
  //  // MOVE: coalesce by entityId; immediate ACK
  //  @Override
  //  public CommandResult storeMoveIntent(MoveRequest move) {
  //    // Keep only the latest for each entity before next tick
  //    moveIntents.put(move.playerId(), move);
  //    return new CommandResult(true, move.playerId(), state.worldName(), null);
  //  }
  //
  //  // JOIN: choose atomic (await) or immediate-ACK depending on caller
  //  @Override
  //  public CommandResult add(JoinRequest joinRequest) {
  //    submitControl(new JoinOp(joinRequest));
  //    return new CommandResult(true, joinRequest.playerName(), state.worldName(), null);
  //  }
  //
  //  @Override
  //  public CompletableFuture<CommandResult> addAsync(JoinRequest joinRequest) {
  //    return submitControlAsync(new JoinOp(joinRequest));
  //  }
  //
  //  @Override
  //  public void remove(String entityId) {
  //    submitControl(new RemoveOp(entityId));
  //  }
  //
  //  @Override
  //  public String serialize() { return engine.serialize(state); }
  //  @Override
  //  public String worldName() { return state.worldName(); }
  //  @Override
  //  public String worldType() { return state.worldType(); }
  //  @Override
  //  public List<Entity> getCurrentEntities() { return state.entities().values().stream().toList(); }
  //  @Override
  //  public WorldCapabilities getCapabilities() { return worldCapabilities; }
  //
  //  @Override
  //  public void reset() {
  //    engine.reset(state);
  //    controlQueue.clear();
  //    moveIntents.clear();
  //  }
  //}

}
