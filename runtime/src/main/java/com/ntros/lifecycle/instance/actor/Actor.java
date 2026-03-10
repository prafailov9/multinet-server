package com.ntros.lifecycle.instance.actor;


import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.lifecycle.Shutdownable;
import com.ntros.lifecycle.session.Session;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.ServerResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.request.MoveRequest;
import com.ntros.model.world.protocol.request.RemoveRequest;
import java.util.concurrent.CompletableFuture;

/**
 * Single-threaded actor, async running all world changes
 */
public interface Actor extends Shutdownable {

  /**
   * updates the world state.
   */
  CompletableFuture<Object> step(WorldConnector world) ;

    CompletableFuture<Void> step(WorldConnector world,
      Runnable onAfterUpdate);

  CompletableFuture<ServerResult> join(WorldConnector world,
      JoinRequest joinRequest);

  CompletableFuture<ServerResult> stageMove(WorldConnector world,
      MoveRequest moveRequest);

  CompletableFuture<ServerResult> remove(WorldConnector world,
      RemoveRequest removeRequest);

  CompletableFuture<Void> tell(Runnable action);

  /**
   * Remove entity (if any) and deregister session on the actor thread
   */
  CompletableFuture<Void> leave(WorldConnector world, SessionManager manager,
      Session session);

  boolean isRunning();
}
