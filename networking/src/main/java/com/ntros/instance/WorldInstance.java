package com.ntros.instance;

import com.ntros.event.listener.SessionManager;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.server.scheduler.ServerTickScheduler;
import com.ntros.server.scheduler.TickScheduler;
import com.ntros.session.Session;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Layer that allows the world to interact with clients. Unique per world connector + session
 * manager.
 */

// TODO: Explore Refactor for Instance to BE the TickScheduler
@Slf4j
public class WorldInstance implements Instance {

  private final WorldConnector worldConnector;
  private final SessionManager sessionManager;
  private final TickScheduler tickScheduler;
  private final AtomicBoolean tickSchedulerRunning = new AtomicBoolean(false);


  public WorldInstance(WorldConnector worldConnector, SessionManager sessionManager) {
    this.worldConnector = worldConnector;
    this.sessionManager = sessionManager;
    tickScheduler = new ServerTickScheduler(120);
  }

  public WorldInstance(WorldConnector worldConnector, SessionManager sessionManager,
      TickScheduler tickScheduler) {
    this.worldConnector = worldConnector;
    this.sessionManager = sessionManager;
    this.tickScheduler = tickScheduler;
  }

  @Override
  public String worldName() {
    return worldConnector.worldName();
  }

  @Override
  public void run() {
    log.info("[IN WORLD INSTANCE]: Updating {} state...", worldName());
    tickScheduler.tick(() -> {
      worldConnector.update();

      String stateMessage = "STATE " + worldConnector.serialize();
      log.info("Broadcasting server response to clients:\n {}", stateMessage);

      sessionManager.broadcast(stateMessage);
    });
    tickSchedulerRunning.set(true);
  }

  @Override
  public void reset() {
    log.info("Resetting World {}... stopping active sessions.", worldName());
    tickScheduler.shutdown();
    tickSchedulerRunning.set(false);
    sessionManager.shutdownAll();
    worldConnector.reset();
  }

  @Override
  public void registerSession(Session session) {
    sessionManager.register(session);
  }

  @Override
  public void removeSession(Session session) {
    sessionManager.remove(session);
  }

  @Override
  public int getActiveSessionsCount() {
    return sessionManager.activeSessionsCount();
  }

  @Override
  public boolean isRunning() {
    return tickSchedulerRunning.get();
  }


}
