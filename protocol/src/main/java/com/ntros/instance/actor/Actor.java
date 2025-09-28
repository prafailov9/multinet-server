package com.ntros.instance.actor;


import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.request.RemoveRequest;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.session.Session;
import java.util.concurrent.CompletableFuture;

/**
 * Single-threaded actor, async running all world changes
 */
public interface Actor {

  /**
   * updates the world state.
   */
  CompletableFuture<Void> step(WorldConnector world,
      Runnable onAfterUpdate);

  CompletableFuture<CommandResult> join(WorldConnector world,
      JoinRequest joinRequest);

  CompletableFuture<CommandResult> stageMove(WorldConnector world,
      MoveRequest moveRequest);

  CompletableFuture<CommandResult> remove(WorldConnector world,
      RemoveRequest removeRequest);

  CompletableFuture<Void> tell(Runnable action);

  /**
   * Remove entity (if any) and deregister session on the actor thread
   */
  CompletableFuture<Void> leave(WorldConnector world, SessionManager manager,
      Session session);

  boolean isRunning();

  void shutdown();

}
