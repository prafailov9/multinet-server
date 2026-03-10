package com.ntros.lifecycle.instance.actor;

import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.lifecycle.session.Session;
import com.ntros.model.entity.Direction;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.connector.ops.JoinOp;
import com.ntros.model.world.connector.ops.MoveOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.request.RemoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CommandActor implements Actor {

    private final ExecutorService control;

    /**
     * Indicates whether the actor still accepts new work.
     * Prevents tasks from being submitted after shutdown begins.
     */
    private final AtomicBoolean accepting = new AtomicBoolean(true);

    /**
     * last-write-wins move coalescing
     */
    private final ConcurrentHashMap<String, Direction> stagedMoves = new ConcurrentHashMap<>();

    public CommandActor(String worldName) {
        this(true, worldName);
    }

    public CommandActor(boolean runInBackground, String worldName) {
        this.control = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "actor-" + worldName + "-ctl");
            t.setDaemon(runInBackground);
            return t;
        });
    }

    /**
     * One tick executed entirely on the actor thread.
     */
    public CompletableFuture<Void> step(WorldConnector world, Runnable onAfterUpdate) {
        return ask(() -> {
            applyMoves(world);
            world.update();
            onAfterUpdate.run();
            return null;
        });
    }

    @Override
    public CompletableFuture<CommandResult> join(WorldConnector world, JoinRequest join) {
        return ask(() -> world.apply(new JoinOp(join)));
    }

    @Override
    public CompletableFuture<CommandResult> stageMove(WorldConnector world, MoveRequest move) {
        stagedMoves.put(move.playerId(), move.direction());
        return CompletableFuture.completedFuture(
                CommandResult.succeeded(move.playerId(), world.getWorldName(), "queued"));
    }

    @Override
    public CompletableFuture<Void> leave(WorldConnector world, SessionManager manager,
                                         Session session) {

        return ask(() -> {
            var ctx = session.getSessionContext();

            if (ctx.getEntityId() != null) {
                world.apply(new RemoveOp(new RemoveRequest(ctx.getEntityId())));
                stagedMoves.remove(ctx.getEntityId());
            }

            manager.remove(session);
            return null;
        });
    }

    @Override
    public CompletableFuture<CommandResult> remove(WorldConnector world, RemoveRequest req) {
        return ask(() -> world.apply(new RemoveOp(req)));
    }

    /**
     * Safe actor execution helper.
     * <p>
     * Guarantees:
     * - never throws RejectedExecutionException
     * - completes future exceptionally instead
     */
    private <T> CompletableFuture<T> ask(Supplier<T> action) {

        CompletableFuture<T> promise = new CompletableFuture<>();

        if (!accepting.get()) {
            promise.completeExceptionally(
                    new IllegalStateException("Actor shutting down"));
            return promise;
        }

        try {

            control.execute(() -> {
                try {
                    promise.complete(action.get());
                } catch (Throwable t) {
                    promise.completeExceptionally(t);
                }
            });

        } catch (RejectedExecutionException ex) {

            promise.completeExceptionally(
                    new IllegalStateException("Actor executor terminated", ex));
        }

        return promise;
    }

    @Override
    public CompletableFuture<Void> tell(Runnable task) {
        return ask(() -> {
            task.run();
            return null;
        });
    }

    /**
     * Allows tests or shutdown code to wait until all queued tasks finish.
     */
    public CompletableFuture<Void> drain() {
        return tell(() -> {
        });
    }

    @Override
    public boolean isRunning() {
        return accepting.get();
    }

    /**
     * Graceful shutdown.
     * <p>
     * Steps:
     * 1. stop accepting new tasks
     * 2. let queued tasks finish
     * 3. terminate executor
     */
    @Override
    public void shutdown() {

        if (!accepting.compareAndSet(true, false)) {
            log.info("Actor already shutting down");
            return;
        }

        control.shutdown();

        try {
            if (!control.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Actor did not terminate gracefully, forcing shutdown");
                control.shutdownNow();
            }
        } catch (InterruptedException e) {
            control.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void applyMoves(WorldConnector world) {
        if (!stagedMoves.isEmpty()) {
            for (var e : stagedMoves.entrySet()) {
                world.apply(new MoveOp(new MoveRequest(e.getKey(), e.getValue())));
            }
            stagedMoves.clear();
        }
    }

}