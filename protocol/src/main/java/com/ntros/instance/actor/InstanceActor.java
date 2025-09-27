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
public interface InstanceActor {

  CompletableFuture<CommandResult> join(WorldConnector worldConnector,
      JoinRequest joinRequest);

  CompletableFuture<CommandResult> move(WorldConnector worldConnector,
      MoveRequest moveRequest);

  CompletableFuture<CommandResult> remove(WorldConnector worldConnector,
      RemoveRequest removeRequest);

  boolean isRunning();

  CompletableFuture<Void> execute(Runnable action);

  /**
   * Remove entity (if any) and deregister session on the actor thread
   */
  CompletableFuture<Void> leave(WorldConnector worldConnector, SessionManager sessionManager,
      Session session);

  void stopActor();

}
